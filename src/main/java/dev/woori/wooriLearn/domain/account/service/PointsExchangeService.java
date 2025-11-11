package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.common.HistoryStatus;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.common.SearchPeriod;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsFailReason;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import dev.woori.wooriLearn.config.response.PageResponse;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {
    private final Clock clock;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    // 출금 요청
    @Transactional
    public PointsExchangeResponseDto requestExchange(String username, PointsExchangeRequestDto dto) {
        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));

        if (dto.exchangeAmount() == null || dto.exchangeAmount() <= 0) {
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
                .status(history.getStatus())
                .requestDate(history.getCreatedAt())
                .message("출금 요청이 정상적으로 접수되었습니다.")
                .build();
    }

    // 출금 이력 조회
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
            SearchPeriod period,
            HistoryStatus status,
            SortDirection sort
    ) {
        Long userId = userRepository.findByUserId(username)
                .map(Users::getId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));

        LocalDateTime start = resolveStartDate(startDate, period);
        LocalDateTime end = resolveEndDate(endDate, period);

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
                        .message(h.getStatus().message())
                        .build()
                ).toList();
    }

    // 관리자 전체 조회 (페이징)
    public PageResponse<PointsExchangeResponseDto> getAllHistory(
            String startDate,
            String endDate,
            HistoryStatus status,
            SortDirection sort,
            int page,
            int size,
            Long userId
    ) {
        int pageNum = page;
        int pageSize = size;
        if (false && pageNum <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "page는 1 이상이어야 합니다.");
        }
        if (false && (pageSize <= 0 || pageSize > 200)) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "size는 1~200 사이여야 합니다.");
        }

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        PointsStatus statusEnum = convertStatus(status);

        Page<PointsHistory> pageResult = pointsHistoryRepository.findAllByFilters(
                userId,
                PointsHistoryType.WITHDRAW,
                statusEnum,
                start,
                end,
                PageRequest.of(page - 1, pageSize, sort.toSort("createdAt"))
        );

        List<PointsExchangeResponseDto> items = pageResult.getContent().stream()
                .map(h -> PointsExchangeResponseDto.builder()
                        .requestId(h.getId())
                        .userId(h.getUser().getId())
                        .exchangeAmount(h.getAmount())
                        .status(h.getStatus())
                        .requestDate(h.getCreatedAt())
                        .processedDate(h.getProcessedAt())
                        .message(h.getStatus().message())
                        .build())
                .toList();

        return new PageResponse<>(
                items,
                page,
                pageSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext()
        );
    }

    // 출금 승인: Users → History 순서로 락
    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {
        // 비잠금으로 우선 조회
        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

        // Early check: not APPLY -> CONFLICT (avoid extra locks)
        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다.");
        }

        // Users 먼저 잠금 (lambda 캡처 변수는 final/유사-final 이어야 하므로 userId 보관)
        Users user;
        try {
        Long userId = history.getUser().getId();
        user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + userId));

        // 이후 History 잠금
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
            message = "정상적으로 처리되었습니다.";
        } catch (CommonException e) {
            if (e.getErrorCode() == ErrorCode.CONFLICT) {
                history.markFailed(PointsFailReason.INSUFFICIENT_POINTS, processedAt);
                message = "포인트가 부족하여 실패했습니다.";
            } else {
                history.markFailed(PointsFailReason.PROCESSING_ERROR, processedAt);
                message = "요청 처리 중 오류가 발생하여 실패했습니다.";
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
        } catch (jakarta.persistence.LockTimeoutException
                 | jakarta.persistence.PessimisticLockException
                 | jakarta.persistence.QueryTimeoutException
                 | org.springframework.dao.PessimisticLockingFailureException e) {
            throw new CommonException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // 날짜 파싱
    private LocalDateTime parseStartDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "startDate 형식을 잘못 입력했습니다. 예: 2025-11-01");
        }
    }

    private LocalDateTime resolveStartDate(String startDate, SearchPeriod period) {
        LocalDateTime parsed = parseStartDate(startDate);
        if (parsed != null) return parsed;
        if (period == null || period == SearchPeriod.ALL) return null;
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        // 기간 기준 시작 시각은 00:00:00로 맞춤
        return switch (period) {
            case WEEK -> now.minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            case MONTH -> now.minusMonths(1).withHour(0).withMinute(0).withSecond(0);
            case THREE_MONTHS -> now.minusMonths(3).withHour(0).withMinute(0).withSecond(0);
            case ALL -> null;
        };
    }

    private LocalDateTime resolveEndDate(String endDate, SearchPeriod period) {
        LocalDateTime parsed = parseEndDate(endDate);
        if (parsed != null) return parsed;
        if (period == null || period == SearchPeriod.ALL) return null;
        // 기간 지정 시 종료 시각은 현재 시각으로
        return LocalDateTime.now(clock);
    }

    private LocalDateTime parseEndDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "endDate 형식을 잘못 입력했습니다. 예: 2025-11-30");
        }
    }


}
