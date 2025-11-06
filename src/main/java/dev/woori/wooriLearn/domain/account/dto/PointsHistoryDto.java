package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointsHistoryDto {

    private Long historyId;
    private Integer amount;
    private PointsHistoryType type;    // DEPOSIT / WITHDRAW
    private PointsStatus status;       // APPLY / SUCCESS / FAILED
    private LocalDateTime createdAt;   // 내역 생성일
    private LocalDateTime processedAt; // 환전 완료/실패 시
    private String failReason;         // 실패 이유(환전 실패)
}
