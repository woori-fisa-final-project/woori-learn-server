package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.common.HistoryStatus;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.common.SearchPeriod;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistorySearchRequestDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsFailReason;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
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
            throw new CommonException(ErrorCode.INVALID_REQUEST, "교환 요청 금액은 0보다 커야 합니다");
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

    // 특정 사용자의 포인트 내역 확인
    public PageResponse<PointsExchangeResponseDto> getUserHistoryPage(
            String username,
            PointsHistorySearchRequestDto request
    ) {
        Long userId = userRepository.findByUserId(username)
                .map(Users::getId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));
        return getHistoryPage(userId, request);
    }

    // 관리자 전용 - 전체 포인트 내역 확인
    public PageResponse<PointsExchangeResponseDto> getAdminHistoryPage(PointsHistorySearchRequestDto request) {
        return getHistoryPage(request.userId(), request);
    }

    // 포인트 이력 조회(사용자/관리자 통합, 페이지네이션 적용)
    public PageResponse<PointsExchangeResponseDto> getHistoryPage(
           Long userId, PointsHistorySearchRequestDto requestDto
    ) {
        // 입력값 검증
        validatePagination(requestDto.page(), requestDto.size());

        DateRange range = resolveDateRange(requestDto.startDate(), requestDto.endDate(), requestDto.period());
        PointsStatus statusEnum = convertStatus(requestDto.status());

        Page<PointsHistory> pageResult = pointsHistoryRepository.findAllByFilters(
                userId,
                PointsHistoryType.WITHDRAW,
                statusEnum,
                range.start,
                range.end,
                PageRequest.of(requestDto.page() - 1, requestDto.size(), requestDto.sort().toSort("createdAt"))
        );

        List<PointsExchangeResponseDto> items = pageResult.getContent().stream()
                .map(PointsExchangeResponseDto::from)
                .toList();

        return PageResponse.of(pageResult, items);
    }

    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {
        // 비잠금으로 우선 조회
        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId));

        // Early check: not APPLY -> CONFLICT (avoid extra locks)
        if (history.getStatus() != PointsStatus.APPLY) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 처리된 요청입니다");
        }

        // Users 먼저 잠금
        Users user;
        try {
        Long userId = history.getUser().getId();
        user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. Id=" + userId));

        // 이후 History 잠금
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
                message = "포인트가 부족하여 실패했습니다.";
            } else {
                history.markFailed(PointsFailReason.PROCESSING_ERROR, processedAt);
                message = "요청 처리 도중 오류가 발생하여 실패했습니다.";
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
                    "처리가 지연되었습니다. 잠시 후 다시 시도해주세요");
        }
    }

    // 페이지네이션 입력값 검증
    private void validatePagination(int page, int size) {
        if (page <= 0) throw new CommonException(ErrorCode.INVALID_REQUEST, "page는 1 이상이어야 합니다");
        if (size <= 0 || size > 200) throw new CommonException(ErrorCode.INVALID_REQUEST, "size는 1~200 이어야 합니다");
    }

    // 상태 변경
    private PointsStatus convertStatus(HistoryStatus status) {
        if (status == null || status == HistoryStatus.ALL) return null;
        return switch (status) {
            case APPLY -> PointsStatus.APPLY;
            case SUCCESS -> PointsStatus.SUCCESS;
            case FAILED -> PointsStatus.FAILED;
            default -> throw new CommonException(ErrorCode.INVALID_REQUEST, "Unknown status: " + status);
        };
    }

    // 기간 확인
    private DateRange resolveDateRange(String startDate, String endDate, SearchPeriod period) {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        boolean noExplicitDates = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty());

        if(period != null && period != SearchPeriod.ALL && noExplicitDates) {
            start = switch (period) {
                case WEEK -> now.minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
                case MONTH -> now.minusMonths(1).withHour(0).withMinute(0).withSecond(0);
                case THREE_MONTHS -> now.minusMonths(3).withHour(0).withMinute(0).withSecond(0);
                default -> null;
            };
            end = now;
        }else if(start != null && end == null) {
            end = now;
        }else if (end != null && start == null) {
            start = end.minusMonths(1).withHour(0).withMinute(0).withSecond(0);
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {}

    // 날짜 파싱
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

}
