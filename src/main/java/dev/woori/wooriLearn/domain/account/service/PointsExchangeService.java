package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {

    private final PointsHistoryRepository pointsHistoryRepository;
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;

    /* ✅ 출금 신청 */
    @Transactional
    public PointsExchangeResponseDto requestExchange(Long userId, PointsExchangeRequestDto dto) {

        Users user = usersRepository.findById(dto.getDbId())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        Account account = accountRepository.findByAccountNumber(dto.getAccountNum())
                .orElseThrow(() -> new IllegalArgumentException("계좌가 존재하지 않습니다."));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("사용자 소유의 계좌가 아닙니다.");
        }

        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.getExchangeAmount())
                        .type(PointsHistoryType.WITHDRAW)       // ✅ 새 필드
                        .status(PointsStatus.APPLY)             // ✅ 신청 상태
                        .build()
        );

        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getAmount())
                .status(history.getStatus())
                .requestDate(history.getCreatedAt().toString())    // ✅ paymentDate → createdAt
                .message("현금화 요청이 정상적으로 접수되었습니다.")
                .build();
    }

    /* ✅ 출금 내역 조회 */
    public List<PointsExchangeResponseDto> getHistory(
            Long userId,
            String startDate,
            String endDate,
            String status,
            String sort
    ) {
        LocalDateTime start = (startDate != null && !startDate.isEmpty())
                ? LocalDate.parse(startDate).atStartOfDay()
                : null;

        LocalDateTime end = (endDate != null && !endDate.isEmpty())
                ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : null;

        PointsStatus statusEnum = null;

        if (!status.equalsIgnoreCase("ALL")) {
            try {
                statusEnum = PointsStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return List.of(
                        PointsExchangeResponseDto.builder()
                                .userId(userId)
                                .message("잘못된 status 값입니다. 가능한 값: ALL / APPLY / SUCCESS / FAILED")
                                .build()
                );
            }
        }

        Sort sortOption = sort.equalsIgnoreCase("ASC") ?
                Sort.by("createdAt").ascending() :
                Sort.by("createdAt").descending();

        List<PointsHistory> list = pointsHistoryRepository.findByFilters(
                userId, statusEnum, start, end, sortOption
        );

        return list.stream()
                .filter(h -> h.getType() == PointsHistoryType.WITHDRAW)  // ✅ 출금 기록만 필터링
                .map(h -> PointsExchangeResponseDto.builder()
                        .requestId(h.getId())
                        .userId(h.getUser().getId())
                        .exchangeAmount(h.getAmount())
                        .status(h.getStatus())
                        .requestDate(h.getCreatedAt().toString())
                        .processedDate(h.getProcessedAt())
                        .message(h.getStatus().toString())
                        .build()
                ).toList();
    }

    /* ✅ 출금 승인/처리 */
    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {
        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("해당 요청이 존재하지 않습니다."));

        if (history.getStatus() != PointsStatus.APPLY) {
            throw new RuntimeException("이미 처리된 요청입니다.");
        }

        Users user = usersRepository.findById(history.getUser().getId())
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        int amount = history.getAmount();

        /* ✅ 포인트 부족 */
        if (user.getPoints() < amount) {
            history.markFailed("포인트가 부족하여 실패했습니다.");
            return PointsExchangeResponseDto.builder()
                    .requestId(requestId)
                    .userId(user.getId())
                    .exchangeAmount(amount)
                    .status(PointsStatus.FAILED)
                    .message("포인트가 부족하여 실패했습니다.")
                    .processedDate(history.getProcessedAt())
                    .build();
        }

        /* ✅ SUCCESS */
        user.setPoints(user.getPoints() - amount);
        usersRepository.save(user);

        history.markSuccess();

        return PointsExchangeResponseDto.builder()
                .requestId(requestId)
                .userId(user.getId())
                .exchangeAmount(amount)
                .status(PointsStatus.SUCCESS)
                .message("정상적으로 처리되었습니다.")
                .processedDate(history.getProcessedAt())
                .build();
    }
}
