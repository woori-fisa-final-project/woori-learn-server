package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositResponseDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointsDepositService {

    private final UsersRepository usersRepository;
    private final PointsHistoryRepository pointsHistoryRepository;

    @Transactional
    public PointsDepositResponseDto depositPoints(PointsDepositRequestDto dto) {

        Users user = usersRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 1) 포인트 증가
        user.addPoints(dto.getAmount());
        usersRepository.save(user);

        // 2) 내역 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.getAmount())
                        .type(PointsHistoryType.DEPOSIT)
                        .status(PointsStatus.SUCCESS)
                        .build()
        );

        return PointsDepositResponseDto.builder()
                .userId(user.getId())
                .addedPoint(dto.getAmount())
                .currentBalance(user.getPoints())
                .status("SUCCESS")
                .message(dto.getReason() != null ? dto.getReason() : "포인트 적립 완료")
                .depositDate(history.getCreatedAt())
                .build();
    }
}
