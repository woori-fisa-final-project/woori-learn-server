package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.external.request.BankTransferReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.dto.request.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.entity.*;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsExchangeService {

    private final Clock clock;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountClient accountClient;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    @Value("${app.admin.account-number}")
    private String adminAccountNumber;
    /**
     * 처리 순서
     * 1) 사용자 행 잠금 조회 (for update)
     * 2) 요청 금액/잔액 검증
     * 3) 출금 계좌 소유자 검증
     * 4) 출금 APPLY 이력 저장
     * 5) 응답 DTO 구성
     */
    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {

        // 1) 사용자 행 잠금 조회
        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + username
                ));

        // 2) 요청 금액/잔액 검증
        if (dto.exchangeAmount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "교환 요청 금액이 0보다 커야 합니다");
        }
        if (user.getPoints() < dto.exchangeAmount()) {
            throw new CommonException(ErrorCode.CONFLICT, "포인트가 부족하여 출금 요청을 처리할 수 없습니다.");
        }

        // 3) 출금 계좌 소유자 검증
        Account account = getValidateAccount(dto.accountNum(), user.getId());

        // 선차감
        user.subtractPoints(dto.exchangeAmount());

        // 4) 출금 APPLY 이력 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.exchangeAmount())
                        .type(PointsHistoryType.WITHDRAW)
                        .status(PointsStatus.APPLY)
                        .accountNumber(account.getAccountNumber())
                        .build()
        );

        // 5) 응답 DTO 구성
        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .currentBalance(user.getPoints())
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("출금 요청이 정상적으로 접수되었습니다.")
                .build();
    }

    /**
     * 처리 프로세스
     * 1) 출금 히스토리 조회 및 상태 확인(APPLY)
     * 2) 사용자/히스토리 재잠금 후 검증
     * 3) 포인트 차감 시도 및 성공/실패 기록
     * 4) 응답 DTO 구성
     */

    /**
     * STEP 1: PROCESSING 상태로 변경 (즉시 커밋)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsProcessing(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "출금 요청을 찾을 수 없습니다. requestId=" + requestId
                ));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다.");
        }

        history.markProcessing();
    }

    /**
     * STEP 2: 은행 이체 + 상태 반영
     */
    @Transactional
    public PointsExchangeResponseDto executeTransfer(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "출금 요청을 찾을 수 없습니다. requestId=" + requestId
                ));

        // 사용자 유효성 검사
        Users user = userRepository.findByIdForUpdate(history.getUser().getId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + history.getUser().getId()));

        // 계좌번호 유효성 검사
        Account account = getValidateAccount(history.getAccountNumber(), user.getId());

        int amount = history.getAmount();
        LocalDateTime now = LocalDateTime.now(clock);

        try {
            BankTransferReqDto bankReq = new BankTransferReqDto(
                    adminAccountNumber,
                    account.getAccountNumber(),
                    (long) amount
            );

            BankTransferResDto bankRes = accountClient.transfer(bankReq);

            if (bankRes != null && bankRes.code() == 200) {
                history.markSuccess(now);
                return buildResponse(history, user, "정상적으로 처리되었습니다.");
            }

            // 은행에서 실패 응답 → 환불 후 FAILED
            user.addPoints(amount);
            history.markFailed(PointsFailReason.PROCESSING_ERROR, now);

            return buildResponse(history, user, "은행 서버에서 이체 실패가 발생했습니다. 포인트가 환불되었습니다.");

        } catch (RestClientException  e) {
            /**
             *  네트워크 오류 → 은행이 돈을 보냈는지 알 수 없음
             *   → FAILED 또는 SUCCESS로 단정할 수 없음
             *   → PROCESSING 유지가 가장 안전
             */
            log.error("[PROCESSING 유지] 은행 서버 통신 오류. requestId={}", requestId, e);

            return buildResponse(history, user,
                    "은행 서버 확인 중입니다. 잠시 후 다시 확인해주세요.");
        }
    }

    private PointsExchangeResponseDto buildResponse(PointsHistory history, Users user, String message) {
        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .currentBalance(user.getPoints())
                .status(history.getStatus())
                .message(message)
                .processedDate(history.getProcessedAt())
                .build();
    }

    /**
     * 관리자용: 환전 신청(APPLY) 전체 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<PointsHistoryResponseDto> getPendingWithdrawals(Integer page, Integer size) {

        int pageNumber = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        PageRequest pageRequest = PageRequest.of(
                pageNumber - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return pointsHistoryRepository.findByTypeAndStatus(
                        PointsHistoryType.WITHDRAW,
                        PointsStatus.APPLY,
                        pageRequest
                )
                .map(PointsHistoryResponseDto::new);
    }

    private Account getValidateAccount(String accountNumber, Long userId){
        // 1. 계좌 조회
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "계좌를 찾을 수 없습니다. accountNum=" + accountNumber
                ));

        // 2. 소유주 검증
        if (!account.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN, "해당 계좌의 소유자가 아닙니다.");
        }

        return account;
    }
}