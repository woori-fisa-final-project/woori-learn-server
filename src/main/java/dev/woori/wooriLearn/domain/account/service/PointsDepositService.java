package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.UserNotFoundException;
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
    public PointsDepositResponseDto depositPoints(Long userId, PointsDepositRequestDto dto) {

        Users user = usersRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 1) 포인트 증가
        user.addPoints(dto.amount());

        // 2) 내역 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.amount())
                        .type(PointsHistoryType.DEPOSIT)
                        .status(PointsStatus.SUCCESS)
                        .build()
        );

        // 3) 응답 반환
        return PointsDepositResponseDto.builder()
                .userId(user.getId())
                .addedPoint(dto.amount())
                .currentBalance(user.getPoints())
                .status(PointsStatus.SUCCESS)
                .message(dto.reason() != null ? dto.reason() : "포인트 적립 완료")
                .createdAt(history.getCreatedAt())
                .build();
    }
}
