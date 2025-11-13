package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public record PointsDepositResponseDto(
        Long userId,
        Integer addedPoint,
        Integer currentBalance,
        PointsStatus status,
        String message,
        LocalDateTime createdAt
) {}
