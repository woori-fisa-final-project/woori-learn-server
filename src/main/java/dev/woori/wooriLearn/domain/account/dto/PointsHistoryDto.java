package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;

import java.time.LocalDateTime;

public record PointsHistoryDto(
        Integer amount,
        PointsHistoryType type,
        PointsStatus status,
        LocalDateTime processedAt
) {
    public static PointsHistoryDto from(PointsHistory entity) {
        return new PointsHistoryDto(
                entity.getAmount(),
                entity.getType(),
                entity.getStatus(),
                entity.getProcessedAt()
        );
    }
}
