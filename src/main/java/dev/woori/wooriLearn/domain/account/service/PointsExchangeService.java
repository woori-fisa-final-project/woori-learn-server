package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.ExchangeProcessContext;
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
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final CacheManager cacheManager;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 처리 절차
     * 1) 사용자 포인트 조회 (for update)
     * 2) 신청 금액/잔액 검증
     * 3) 출금 계좌 소유자 검증
     * 4) 출금 APPLY 이력 생성
     * 5) 응답 DTO 구성
     */
    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {

        // 1) 사용자 포인트 조회
        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + username
                ));

        // 2) 신청 금액/잔액 검증
        if (dto.exchangeAmount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "교환 요청 금액은 0보다 커야 합니다");
        }
        if (user.getPoints() < dto.exchangeAmount()) {
            throw new CommonException(ErrorCode.CONFLICT, "포인트가 부족하여 출금 신청을 처리할 수 없습니다.");
        }

        // 3) 출금 계좌 소유자 검증
        Account account = getValidateAccount(dto.accountNum(), user.getId());

        // 차감
        user.subtractPoints(dto.exchangeAmount());

        // 4) 출금 APPLY 이력 생성
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.exchangeAmount())
                        .type(PointsHistoryType.WITHDRAW)
                        .status(PointsStatus.APPLY)
                        .accountNumber(account.getAccountNumber())
                        .build()
        );

        // Evict user info cache after points subtraction
        {
            var cache = cacheManager.getCache("userInfo_v2");
            if (cache != null) cache.evict(username);
        }

        // 5) 응답 DTO 구성
        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .currentBalance(user.getPoints())
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("출금 신청이 정상적으로 접수되었습니다.")
                .build();
    }

    /**
     * 외부 api로 전송 준비
     * lock 설정 + 검증 + Process 상태로 전환
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExchangeProcessContext prepareTransfer(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "출금 신청을 찾을 수 없습니다. requestId=" + requestId
                ));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
        }

        // 사용자 유효성 검증
        Users user = userRepository.findByIdForUpdate(history.getUser().getId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + history.getUser().getId()));

        // 계좌번호 유효성 검증
        Account account = getValidateAccount(history.getAccountNumber(), user.getId());

        // 상태를 Processing으로 변경
        history.markProcessing();

        return ExchangeProcessContext.builder()
                .requestId(requestId)
                .userId(user.getId())
                .accountNum(account.getAccountNumber())
                .amount(history.getAmount())
                .build();
    }

    // 결제 응답을 받은 경우
    @Transactional
    public PointsExchangeResponseDto processResult(Long requestId, BankTransferResDto bankRes){
        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                        "출금 신청을 찾을 수 없습니다. requestId=" + requestId));
        Users user = userRepository.findByUserIdForUpdate(history.getUser().getUserId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now(clock);

        if (bankRes != null && bankRes.code() == 200) {
            history.markSuccess(now);
            // Evict cache to ensure fresh balance on next read
            {
                var cache = cacheManager.getCache("userInfo_v2");
                if (cache != null) cache.evict(user.getUserId());
            }
            return buildResponse(history, user, "정상으로 처리되었습니다.");
        }  else { // 에러 메시지 return
            user.addPoints(history.getAmount());
            history.markFailed(PointsFailReason.PROCESSING_ERROR, now);
            {
                var cache = cacheManager.getCache("userInfo_v2");
                if (cache != null) cache.evict(user.getUserId());
            }
            return buildResponse(history, user, "처리 중 오류가 발생했습니다.");
        }
    }

    // 전송 실패 시
    @Transactional
    public PointsExchangeResponseDto processFailure(Long requestId){
        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND
                        , "출금 신청을 찾을 수 없습니다. requestId=" + requestId));
        Users user = userRepository.findByUserIdForUpdate(history.getUser().getUserId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now(clock);
        user.addPoints(history.getAmount());
        history.markFailed(PointsFailReason.PROCESSING_ERROR, now);
        {
            var cache = cacheManager.getCache("userInfo_v2");
            if (cache != null) cache.evict(user.getUserId());
        }
        return buildResponse(history, user, "백엔드 서버에서 결제 실패가 발생했습니다.");
    }

    public PointsExchangeResponseDto buildResponse(PointsHistory history, Users user, String message) {
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
     * 관리자용 출금 신청(APPLY) 조회 (페이지네이션)
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

        // 2. 소유자 검증
        if (!account.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN, "해당 계좌의 소유자가 아닙니다.");
        }

        return account;
    }
}

