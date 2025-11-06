package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsExchangeStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
                        .paymentAmount(dto.getExchangeAmount())
                        .status(PointsExchangeStatus.APPLY)
                        .build()
        );

        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .userId(user.getId())
                .exchangeAmount(history.getPaymentAmount())
                .status(history.getStatus())
                .requestDate(history.getPaymentDate().toString())
                .message("현금화 요청이 정상적으로 접수되었습니다.")
                .build();
    }

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

        PointsExchangeStatus statusEnum = null;

        if (!status.equalsIgnoreCase("ALL")) {
            try {
                statusEnum = PointsExchangeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return List.of(
                        PointsExchangeResponseDto.builder()
                                .requestId(null)
                                .userId(userId)
                                .exchangeAmount(0)
                                .status(null)
                                .message("잘못된 status 값입니다. 가능한 값: ALL / APPLY / SUCCESS / FAILED")
                                .build()
                );
            }
        }


        List<PointsHistory> list = pointsHistoryRepository.findByFilters(
                userId, statusEnum, start, end
        );

        if (sort.equalsIgnoreCase("ASC")) {
            list.sort((a, b) -> a.getPaymentDate().compareTo(b.getPaymentDate()));
        } else {
            list.sort((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate()));
        }

        if (list.isEmpty()) {
            return List.of(
                    PointsExchangeResponseDto.builder()
                            .requestId(null)
                            .userId(userId)
                            .exchangeAmount(0)
                            .status(null)
                            .requestDate(null)
                            .message("해당 조건에 맞는 환전 내역이 없습니다.")
                            .build()
            );
        }

        return list.stream()
                .map(h -> PointsExchangeResponseDto.builder()
                        .requestId(h.getId())
                        .userId(h.getUser().getId())
                        .exchangeAmount(h.getPaymentAmount())
                        .status(h.getStatus())
                        .requestDate(h.getPaymentDate().toString())
                        .message(h.getStatus().toString())
                        .build()
                ).toList();
    }

    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("해당 요청이 존재하지 않습니다."));

        if (history.getStatus() != PointsExchangeStatus.APPLY) {
            throw new RuntimeException("이미 처리된 요청입니다.");
        }

        // ✅ User row에 DB 락 걸림 (동시성 차단)
        Users user = usersRepository.findByIdForUpdate(history.getUser().getId());

        int amount = history.getPaymentAmount();

        if (user.getPoints() < amount) {
            history.setStatus(PointsExchangeStatus.FAILED);
            history.setProcessedDate(LocalDateTime.now());
            history.setFailReason("포인트가 부족하여 실패했습니다.");
            pointsHistoryRepository.save(history);

            return PointsExchangeResponseDto.builder()
                    .requestId(requestId)
                    .userId(user.getId())
                    .exchangeAmount(amount)
                    .status(PointsExchangeStatus.FAILED)
                    .message("포인트가 부족하여 실패했습니다.")
                    .processedDate(history.getProcessedDate())
                    .build();
        }

        // ✅ 락이 유지된 상태에서 안전하게 차감
        user.setPoints(user.getPoints() - amount);
        history.setStatus(PointsExchangeStatus.SUCCESS);
        history.setProcessedDate(LocalDateTime.now());

        usersRepository.save(user);
        pointsHistoryRepository.save(history);

        return PointsExchangeResponseDto.builder()
                .requestId(requestId)
                .userId(user.getId())
                .exchangeAmount(amount)
                .status(PointsExchangeStatus.SUCCESS)
                .message("정상적으로 처리되었습니다.")
                .processedDate(history.getProcessedDate())
                .build();
    }


}
