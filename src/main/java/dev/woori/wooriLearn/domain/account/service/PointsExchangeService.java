package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.common.HistoryStatus;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {
    private static final String INSUFFICIENT_POINTS_FAIL_REASON = "INSUFFICIENT_POINTS";
    private static final String PROCESSING_ERROR_FAIL_REASON = "PROCESSING_ERROR";
    private final Clock clock;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;


    /* 출금 신청 */
    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {



        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));

        // 금액 유효성 먼저 검증(null/비양수)
        if (dto.exchangeAmount() == null || dto.exchangeAmount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "교환 신청 금액은 0보다 커야 합니다");
        }
        // 사후 잔액 비교: 충족 여부 확인
        if (user.getPoints() < dto.exchangeAmount()) {
            throw new CommonException(ErrorCode.CONFLICT, "포인트가 부족하여 출금 신청을 처리할 수 없습니다.");
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
                .message("출금 신청이 정상적으로 접수되었습니다")
                .build();
    }

    /* 출금 이력 조회 */
    private PointsStatus convertStatus(HistoryStatus status) {
        if (status == null || status == HistoryStatus.ALL) return null;
        return switch (status) {
            case APPLY -> PointsStatus.APPLY;
            case SUCCESS -> PointsStatus.SUCCESS;
            case FAILED -> PointsStatus.FAILED;
            default -> throw new CommonException(ErrorCode.INVALID_REQUEST, "Unknown status: " + status);
        };
    }

    public List<PointsExchangeResponseDto> getHistory(
            String username,
            String startDate,
            String endDate,
            HistoryStatus status,
            SortDirection sort
    ) {
        Long userId = userRepository.findByUserId(username)
                .map(Users::getId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        PointsStatus statusEnum = convertStatus(status);
        Sort sortOption = sort.toSort("createdAt");

        List<PointsHistory> list = pointsHistoryRepository.findByFilters(
                userId, PointsHistoryType.WITHDRAW, statusEnum, start, end, sortOption
        );

        return list.stream()
                .map(h -> PointsExchangeResponseDto.builder()
                        .requestId(h.getId())
                        .userId(userId)
                        .exchangeAmount(h.getAmount())
                        .status(h.getStatus())
                        .requestDate(h.getCreatedAt())
                        .processedDate(h.getProcessedAt())
                        .message(mapStatusToMessage(h.getStatus()))
                        .build()
                ).toList();
    }

    /* 출금 승인 */
    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findAndLockById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다");
        }

        Users user = userRepository.findByIdForUpdate(history.getUser().getId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id= " + history.getUser().getId()));

        int amount = history.getAmount();
        String message;

        if (user.getPoints() < amount) {
            history.markFailed(INSUFFICIENT_POINTS_FAIL_REASON, LocalDateTime.now(clock));
            message = "포인트가 부족하여 실패했습니다.";
        } else {
            user.subtractPoints(amount);
            history.markSuccess(LocalDateTime.now(clock));
            message = "정상적으로 처리되었습니다.";
        }

        return PointsExchangeResponseDto.builder()
                .requestId(requestId)
                .userId(user.getId())
                .exchangeAmount(amount)
                .status(history.getStatus())
                .message(message)
                .processedDate(history.getProcessedAt())
                .build();
    }

    /* 날짜 파싱 */
    private LocalDateTime parseStartDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "startDate 형식을 잘못 입력했습니다. 예) 2025-11-01");
        }
    }

    private LocalDateTime parseEndDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "endDate 형식을 잘못 입력했습니다. 예) 2025-11-30");
        }
    }

    private String mapStatusToMessage(PointsStatus status) {
        return switch (status) {
            case APPLY -> "출금 신청 처리 중입니다.";
            case SUCCESS -> "출금이 완료되었습니다";
            case FAILED -> "출금 신청이 실패했습니다.";
        };
    }
}

