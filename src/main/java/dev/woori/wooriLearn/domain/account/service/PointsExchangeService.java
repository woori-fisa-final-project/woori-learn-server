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

    public PointsExchangeResponseDto requestExchange(Long userId,PointsExchangeRequestDto dto) {

        Users user = usersRepository.findById(dto.getDbId())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        Account account = accountRepository.findByAccountNumber(dto.getAccountNum())
                .orElseThrow(() -> new IllegalArgumentException("계좌가 존재하지 않습니다."));

        // 사용자의 계좌가 맞는지 확인
        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("사용자 소유의 계좌가 아닙니다.");
        }

        // ✅ points_history에 저장
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
    public List<PointsExchangeResponseDto> getHistory(Long userId) {

        List<PointsHistory> list = pointsHistoryRepository.findAllByUser_Id(userId);

        // ✅ 내역 없을 때 안내 메시지 1건 리턴
        if (list.isEmpty()) {
            return List.of(
                    PointsExchangeResponseDto.builder()
                            .requestId(null)
                            .userId(userId)
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
                        .userId(h.getUser().getId())
                        .exchangeAmount(h.getPaymentAmount())
                        .status(h.getStatus())
                        .requestDate(h.getPaymentDate().toString())
                        .message(h.getStatus().toString())
                        .build()
                ).toList();
    }


}
