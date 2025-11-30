package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.external.request.BankTransferReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.dto.request.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsFailReason;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Account account = accountRepository.findByAccountNumber(dto.accountNum())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "계좌를 찾을 수 없습니다. accountNum=" + dto.accountNum()));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new CommonException(ErrorCode.FORBIDDEN, "해당 계좌의 소유자가 아닙니다.");
        }

        // 4) 출금 APPLY 이력 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.exchangeAmount())
                        .type(PointsHistoryType.WITHDRAW)
                        .status(PointsStatus.APPLY)
                        .build()
        );

        // 5) 응답 DTO 구성
        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("출금 요청이 정상적으로 접수되었습니다")
                .build();
    }

    /**
     * 처리 순서
     * 1) 출금 이력 조회 및 상태 확인(APPLY)
     * 2) 사용자/이력 잠금 조회로 동시성 제어
     * 3) 포인트 차감 시도 후 성공/실패 기록
     * 4) 응답 DTO 구성
     */
    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {
        // 1) 출금 이력 조회 및 상태 확인
        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다");
        }

        try {
            // 2) 사용자/이력 잠금 조회
            Long userId = history.getUser().getId();
            Users user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + userId));

            history = pointsHistoryRepository.findAndLockById(requestId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

            if (history.getStatus() != PointsStatus.APPLY) {
                throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다");
            }

            Account account = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new CommonException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "사용자 계좌를 찾을 수 없습니다. userId=" + userId
                    ));

            // 3) 포인트 차감 시도 및 상태 기록
            int amount = history.getAmount();
            String message;
            LocalDateTime processedAt = LocalDateTime.now(clock);
            try {
                BankTransferReqDto bankReq = new BankTransferReqDto(
                        "999900000001",                 // 관리자 계좌 번호
                        account.getAccountNumber(),      // 사용자 계좌 번호 ← 여기!
                        (long) amount
                );

                BankTransferResDto bankRes = accountClient.transfer(bankReq);
                log.info("은행 서버 응답 = {}", bankRes);

                if (bankRes.success()) {
                    user.subtractPoints(amount);
                    history.markSuccess(processedAt);
                    message = "정상적으로 처리되었습니다.";
                } else {
                    history.markFailed(PointsFailReason.PROCESSING_ERROR, processedAt);
                    message = "은행 서버에서 이체 실패가 발생했습니다.";
                }

            } catch (CommonException e) {
                if (e.getErrorCode() == ErrorCode.CONFLICT) {
                    history.markFailed(PointsFailReason.INSUFFICIENT_POINTS, processedAt);
                    message = "포인트가 부족하여 실패했습니다.";
                } else {
                    history.markFailed(PointsFailReason.PROCESSING_ERROR, processedAt);
                    message = "요청 처리 중 오류가 발생하여 실패했습니다.";
                }
            }

            // 4) 응답 DTO 구성
            return PointsExchangeResponseDto.builder()
                    .requestId(requestId)
                    .userId(user.getId())
                    .exchangeAmount(amount)
                    .status(history.getStatus())
                    .message(message)
                    .processedDate(history.getProcessedAt())
                    .build();
        } catch (LockTimeoutException
                 | PessimisticLockException
                 | QueryTimeoutException
                 | PessimisticLockingFailureException e) {
            throw new CommonException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "처리가 지연되었습니다. 잠시 후 다시 시도해 주세요");
        }
    }

    /**
     * 관리자용: 환전 신청(APPLY) 전체 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<PointsHistoryResponseDto> getPendingWithdrawals(Integer page, Integer size) {
        int pageNumber = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return pointsHistoryRepository.findByTypeAndStatus(
                        PointsHistoryType.WITHDRAW,
                        PointsStatus.APPLY,
                        pageRequest
                )
                .map(PointsHistoryResponseDto::new);
    }
}