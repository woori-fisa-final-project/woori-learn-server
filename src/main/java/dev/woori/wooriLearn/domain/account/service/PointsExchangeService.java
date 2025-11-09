package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.AccountNotFoundException;
import dev.woori.wooriLearn.config.exception.ForbiddenException;
import dev.woori.wooriLearn.config.exception.InvalidParameterException;
import dev.woori.wooriLearn.config.exception.InvalidStateException;
import dev.woori.wooriLearn.config.exception.PointsRequestNotFoundException;
import dev.woori.wooriLearn.config.exception.UserNotFoundException;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.common.HistoryStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {

    private final Clock clock;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /* 출금 요청 */
    @Transactional
    public PointsExchangeResponseDto requestExchange(Long userId, PointsExchangeRequestDto dto) {

        Users user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getPoints() < dto.exchangeAmount()) {
            throw new InvalidStateException("포인트가 부족하여 출금 요청을 처리할 수 없습니다.");
        }

        Account account = accountRepository.findByAccountNumber(dto.accountNum())
                .orElseThrow(() -> new AccountNotFoundException(dto.accountNum()));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("해당 계좌는 사용자 소유가 아닙니다.");
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

    /* 출금 내역 조회 */
    private PointsStatus convertStatus(HistoryStatus status) {
        if (status == null || status == HistoryStatus.ALL) return null;
        return switch (status) {
            case APPLY -> PointsStatus.APPLY;
            case SUCCESS -> PointsStatus.SUCCESS;
            case FAILED -> PointsStatus.FAILED;
            default -> throw new InvalidParameterException("Unknown status: " + status);
        };
    }

    public List<PointsExchangeResponseDto> getHistory(
            Long userId,
            String startDate,
            String endDate,
            HistoryStatus status,
            SortDirection sort
    ) {
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
                        .userId(h.getUser().getId())
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
                .orElseThrow(() -> new PointsRequestNotFoundException(requestId));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new InvalidStateException("이미 처리된 요청입니다.");
        }

        Users user = userRepository.findByIdForUpdate(history.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException(history.getUser().getId()));



        int amount = history.getAmount();
        String message;

        try {
            user.subtractPoints(amount);
            history.markSuccess(LocalDateTime.now(clock));
            message = "정상적으로 처리되었습니다.";
        } catch (InvalidStateException e) {
            history.markFailed("INSUFFICIENT_POINTS", LocalDateTime.now(clock));
            message = "포인트가 부족하여 실패했습니다.";
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
            throw new InvalidParameterException("startDate 형식이 잘못되었습니다. 예) 2025-11-01");
        }
    }

    private LocalDateTime parseEndDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new InvalidParameterException("endDate 형식이 잘못되었습니다. 예) 2025-11-30");
        }
    }

    private String mapStatusToMessage(PointsStatus status) {
        return switch (status) {
            case APPLY -> "출금 요청 처리 중입니다.";
            case SUCCESS -> "출금이 완료되었습니다.";
            case FAILED -> "출금 요청에 실패했습니다.";
        };
    }
}
