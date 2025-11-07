package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PointsHistoryDto(
        Long historyId,
        Integer amount,
        PointsHistoryType type,
        PointsStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        String failReason
) {}
