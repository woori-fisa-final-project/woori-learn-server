package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositResponseDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointsDepositService {

    private final UserRepository userRepository;
    private final PointsHistoryRepository pointsHistoryRepository;
    private static final String DEFAULT_DEPOSIT_MESSAGE = "포인트 적립 완료";

    @Transactional
    public PointsDepositResponseDto depositPoints(String username, PointsDepositRequestDto dto) {



        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));

        user.addPoints(dto.amount());

        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.amount())
                        .type(PointsHistoryType.DEPOSIT)
                        .status(PointsStatus.SUCCESS)
                        .build()
        );

        return PointsDepositResponseDto.builder()
                .userId(user.getId())
                .addedPoint(dto.amount())
                .currentBalance(user.getPoints())
                .status(PointsStatus.SUCCESS)
                .message(dto.reason() != null ? dto.reason() : DEFAULT_DEPOSIT_MESSAGE)
                .createdAt(history.getCreatedAt())
                .build();
    }
}

