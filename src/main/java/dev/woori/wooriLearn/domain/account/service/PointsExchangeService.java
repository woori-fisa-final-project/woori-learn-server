package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {
    private final Clock clock;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 처리 프로세스
     * 1) 출금 요청자 금액 조회 (for update)
     * 2) 요청 금액/본인 여부 검증
     * 3) 출금 계좌 소유자 검증
     * 4) 출금 APPLY 히스토리 생성
     * 5) 응답 DTO 구성
     */
    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {
        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "출금자를 찾을 수 없습니다. userId=" + username
                ));

        if (dto.exchangeAmount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "교환 요청 금액은 0보다 커야 합니다.");
        }
        if (user.getPoints() < dto.exchangeAmount()) {
            throw new CommonException(ErrorCode.CONFLICT, "포인트가 부족하여 출금 요청을 처리할 수 없습니다.");
        }

        Account account = accountRepository.findByAccountNumber(dto.accountNum())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "계좌를 찾을 수 없습니다. accountNum=" + dto.accountNum()));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new CommonException(ErrorCode.FORBIDDEN, "해당 계좌의 소유자가 아닙니다.");
        }

        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.exchangeAmount())
                        .type(PointsHistoryType.WITHDRAW)
                        .status(PointsStatus.APPLY)
                        .build()
        );

        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .currentBalance(user.getPoints())
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("Withdrawal request received.")
                .build();
    }

    /**
     * 처리 프로세스
     * 1) 출금 히스토리 조회 및 상태 확인(APPLY)
     * 2) 사용자/히스토리 재잠금 후 검증
     * 3) 포인트 차감 시도 및 성공/실패 기록
     * 4) 응답 DTO 구성
     */
    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {
        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다.");
        }

        try {
            Long userId = history.getUser().getId();
            Users user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + userId));

            history = pointsHistoryRepository.findAndLockById(requestId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

            if (history.getStatus() != PointsStatus.APPLY) {
                throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다.");
            }

            int amount = history.getAmount();
            String message;
            LocalDateTime processedAt = LocalDateTime.now(clock);
            try {
                user.subtractPoints(amount);
                history.markSuccess(processedAt);
                message = "처리가 완료되었습니다.";
            } catch (CommonException e) {
                if (e.getErrorCode() == ErrorCode.CONFLICT) {
                    history.markFailed(PointsFailReason.INSUFFICIENT_POINTS, processedAt);
                    message = "포인트가 부족합니다.";
                } else {
                    history.markFailed(PointsFailReason.PROCESSING_ERROR, processedAt);
                    message = "요청 처리 중 오류가 발생했습니다.";
                }
            }

            return PointsExchangeResponseDto.builder()
                    .requestId(requestId)
                    .userId(user.getId())
                    .exchangeAmount(amount)
                    .currentBalance(user.getPoints())
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
                    "처리가 지연되었습니다. 잠시 후 시도해 주세요."
            );
        }
    }

    /**
     * 관리자용 출금(APPLY) 리스트 조회
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
