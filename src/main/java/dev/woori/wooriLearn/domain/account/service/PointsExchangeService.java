package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
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
import org.springframework.core.env.Environment;
import org.springframework.dao.PessimisticLockingFailureException;
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
    private final Environment env;

    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {
        final String actualUsername = env.acceptsProfiles("dev") ? "dev-user" : username;

        Users user = userRepository.findByUserIdForUpdate(actualUsername)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + actualUsername
                ));

        if (dto.exchangeAmount() == null || dto.exchangeAmount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "교환 요청 금액이 0보다 커야 합니다");
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
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("출금 요청이 정상적으로 접수되었습니다")
                .build();
    }

    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {
        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다");
        }

        try {
            Long userId = history.getUser().getId();
            Users user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + userId));

            history = pointsHistoryRepository.findAndLockById(requestId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

            if (history.getStatus() != PointsStatus.APPLY) {
                throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다");
            }

            int amount = history.getAmount();
            String message;
            LocalDateTime processedAt = LocalDateTime.now(clock);
            try {
                user.subtractPoints(amount);
                history.markSuccess(processedAt);
                message = "정상적으로 처리되었습니다";
            } catch (CommonException e) {
                if (e.getErrorCode() == ErrorCode.CONFLICT) {
                    history.markFailed(PointsFailReason.INSUFFICIENT_POINTS, processedAt);
                    message = "포인트가 부족하여 실패했습니다";
                } else {
                    history.markFailed(PointsFailReason.PROCESSING_ERROR, processedAt);
                    message = "요청 처리 중 오류가 발생하여 실패했습니다";
                }
            }

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
}
