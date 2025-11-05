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

    /**
     * ✅ 포인트 환전 신청
     */
    @Transactional
    public PointsExchangeResponseDto requestExchange(PointsExchangeRequestDto dto) {

        Users user = usersRepository.findById(dto.getDbId()).orElse(null);

        // ✅ 유저가 없으면 저장하지 말고 바로 실패 반환
        if (user == null) {
            return PointsExchangeResponseDto.builder()
                    .requestId(null)
                    .user_id(null)
                    .exchangeAmount(dto.getExchangeAmount())
                    .status(PointsExchangeStatus.FAILED)
                    .message("유저가 존재하지 않아 환전 신청이 실패했습니다.")
                    .build();
        }

        Account account = accountRepository.findByAccountNumber(dto.getAccountNum()).orElse(null);

        // ✅ 계좌 없으면 저장 금지
        if (account == null) {
            return PointsExchangeResponseDto.builder()
                    .requestId(null)
                    .user_id(user.getId())
                    .exchangeAmount(dto.getExchangeAmount())
                    .status(PointsExchangeStatus.FAILED)
                    .message("계좌가 존재하지 않아 환전 신청이 실패했습니다.")
                    .build();
        }

        // ✅ 정상 케이스만 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .paymentAmount(dto.getExchangeAmount())
                        .status(PointsExchangeStatus.APPLY)
                        .build()
        );

        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .user_id(user.getId())
                .exchangeAmount(history.getPaymentAmount())
                .status(history.getStatus())
                .requestDate(history.getPaymentDate().toString())
                .message("현금화 요청이 정상적으로 접수되었습니다.")
                .build();
    }


    /**
     * ✅ 환전 내역 필터 조회
     */
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

        PointsExchangeStatus statusEnum = status.equals("ALL") ?
                null : PointsExchangeStatus.valueOf(status);

        List<PointsHistory> list = pointsHistoryRepository.findByFilters(
                userId,
                statusEnum,
                start,
                end
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
                            .user_id(userId)
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
                        .user_id(h.getUser().getId())
                        .exchangeAmount(h.getPaymentAmount())
                        .status(h.getStatus())
                        .requestDate(h.getPaymentDate().toString())
                        .message(h.getStatus().toString())
                        .build()
                ).toList();
    }


    /**
     * ✅ 은행 서버 승인: 포인트 차감 -> SUCCESS
     */
    @Transactional
    public PointsExchangeResponseDto approveExchange(Long requestId) {

        PointsHistory history = pointsHistoryRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("해당 요청이 존재하지 않습니다."));

        if (history.getStatus() != PointsExchangeStatus.APPLY) {
            throw new RuntimeException("이미 처리된 요청입니다.");
        }

        Users user = history.getUser();
        int amount = history.getPaymentAmount();

        // ✅ 포인트 부족 → 실패 기록
        if (user.getPoints() < amount) {
            history.setStatus(PointsExchangeStatus.FAILED);
            history.setProcessedDate(LocalDateTime.now());
            history.setFailReason("포인트가 부족하여 실패했습니다.");
            pointsHistoryRepository.save(history);

            return PointsExchangeResponseDto.builder()
                    .requestId(requestId)
                    .user_id(user.getId())
                    .exchangeAmount(amount)
                    .status(PointsExchangeStatus.FAILED)
                    .message("포인트가 부족하여 실패했습니다.")
                    .processedDate(history.getProcessedDate())
                    .build();
        }

        // ✅ 포인트 차감 성공
        user.setPoints(user.getPoints() - amount);
        history.setStatus(PointsExchangeStatus.SUCCESS);
        history.setProcessedDate(LocalDateTime.now());

        usersRepository.save(user);
        pointsHistoryRepository.save(history);

        return PointsExchangeResponseDto.builder()
                .requestId(requestId)
                .user_id(user.getId())
                .exchangeAmount(amount)
                .status(PointsExchangeStatus.SUCCESS)
                .message("정상적으로 처리되었습니다.")
                .processedDate(history.getProcessedDate())
                .build();
    }

}
