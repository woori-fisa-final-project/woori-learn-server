package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.common.SearchPeriod;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.entity.HistoryFilter;
import dev.woori.wooriLearn.domain.account.dto.PointsUnifiedHistoryRequestDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryQueryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class PointsHistoryService {

    private final PointsHistoryQueryRepository queryRepository;
    private final UserRepository userRepository;
    private final Environment env;
    private final Clock clock;

    public Page<PointsHistory> getUnifiedHistory(String username, PointsUnifiedHistoryRequestDto request) {

        Long userId = resolveUserId(username, request.userId());

        DateRange range = resolveDateRange(request.startDate(), request.endDate(), request.period());
        TypeStatus ts = mapFilter(request.status());

        SortDirection sort = request.sort();
        PageRequest pageRequest = PageRequest.of(
                request.page() - 1,
                request.size(),
                sort.toSort("createdAt")
        );

        return queryRepository.findAllByFilters(
                userId,
                ts.type,
                ts.status,
                range.start,
                range.end,
                pageRequest
        );
    }

    private Long resolveUserId(String username, Long requestUserId) {
        if (requestUserId != null) return requestUserId;
        if (username == null || username.isEmpty()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "userId or authenticated username is required");
        }
        String actualUsername = env.acceptsProfiles("dev") ? "testuser" : username;
        return userRepository.findByUserId(actualUsername)
                .map(Users::getId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + actualUsername));
    }

    private TypeStatus mapFilter(HistoryFilter filter) {
        if (filter == null || filter == HistoryFilter.ALL) return new TypeStatus(null, null);
        return switch (filter) {
            case DEPOSIT -> new TypeStatus(PointsHistoryType.DEPOSIT, null);
            case WITHDRAW_APPLY -> new TypeStatus(PointsHistoryType.WITHDRAW, PointsStatus.APPLY);
            case WITHDRAW_FAILED -> new TypeStatus(PointsHistoryType.WITHDRAW, PointsStatus.FAILED);
            case WITHDRAW_SUCCESS -> new TypeStatus(PointsHistoryType.WITHDRAW, PointsStatus.SUCCESS);
            case ALL -> new TypeStatus(null, null);
        };
    }

    private DateRange resolveDateRange(String startDate, String endDate, SearchPeriod period) {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        boolean noExplicitDates = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty());

        if (period != null && period != SearchPeriod.ALL && noExplicitDates) {
            start = switch (period) {
                case WEEK -> now.minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
                case MONTH -> now.minusMonths(1).withHour(0).withMinute(0).withSecond(0);
                case THREE_MONTHS -> now.minusMonths(3).withHour(0).withMinute(0).withSecond(0);
                default -> null;
            };
            end = now;
        } else if (start != null && end == null) {
            end = now;
        } else if (end != null && start == null) {
            start = end.minusMonths(1).withHour(0).withMinute(0).withSecond(0);
        }
        return new DateRange(start, end);
    }

    private LocalDateTime parseStartDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "startDate 형식이 올바르지 않습니다. 예) 2025-11-01");
        }
    }

    private LocalDateTime parseEndDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "endDate 형식이 올바르지 않습니다. 예) 2025-11-30");
        }
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {}

    private static class TypeStatus {
        private final PointsHistoryType type;
        private final PointsStatus status;

        private TypeStatus(PointsHistoryType type, PointsStatus status) {
            this.type = type;
            this.status = status;
        }
    }
}
