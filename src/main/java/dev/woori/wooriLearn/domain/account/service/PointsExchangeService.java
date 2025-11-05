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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {

    private final PointsHistoryRepository pointsHistoryRepository;
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;

    public PointsExchangeResponseDto requestExchange(PointsExchangeRequestDto dto) {

        Users user = usersRepository.findById(dto.getDb_id()).orElse(null);
        Account account = accountRepository.findByAccountNumber(dto.getAccountNum()).orElse(null);

        PointsExchangeStatus status;
        String message;

        if (user == null) {
            status = PointsExchangeStatus.FAILED;
            message = "유저가 존재하지 않아 환전 신청이 실패했습니다.";
        } else if (account == null) {
            status = PointsExchangeStatus.FAILED;
            message = "계좌가 존재하지 않아 환전 신청이 실패했습니다.";
        } else {
            status = PointsExchangeStatus.APPLY;
            message = "현금화 요청이 정상적으로 접수되었습니다.";
        }

        // ✅ points_history에 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .paymentAmount(dto.getExchangeAmount())
                        .status(status)
                        .build()
        );

        return PointsExchangeResponseDto.builder()
                .requestId(history.getId())
                .user_id(user != null ? user.getId() : null)
                .exchangeAmount(history.getPaymentAmount())
                .status(history.getStatus())
                .requestDate(history.getPaymentDate().toString())
                .message(message)
                .build();
    }
    public List<PointsExchangeResponseDto> getHistory(Long userId) {

        List<PointsHistory> list = pointsHistoryRepository.findAllByUserId(userId);

        // ✅ 내역 없을 때 안내 메시지 1건 리턴
        if (list.isEmpty()) {
            return List.of(
                    PointsExchangeResponseDto.builder()
                            .requestId(null)
                            .user_id(userId)
                            .exchangeAmount(0)
                            .status(PointsExchangeStatus.FAILED)
                            .requestDate(null)
                            .message("해당 사용자의 환전 내역이 존재하지 않습니다.")
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


}
