package dev.woori.wooriLearn.domain.account.dto.response;

import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointsHistoryResponseDto {

    private Long id;
    private String userId;
    private String nickname;
    private PointsHistoryType type;
    private PointsStatus status;
    private int amount;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public PointsHistoryResponseDto(PointsHistory entity) {
        this.id = entity.getId();
        this.userId = entity.getUser().getUserId();
        this.nickname = entity.getUser().getNickname();
        this.type = entity.getType();
        this.status = entity.getStatus();
        this.amount = entity.getAmount();
        this.createdAt = entity.getCreatedAt();
        this.processedAt = entity.getProcessedAt();
    }
}
