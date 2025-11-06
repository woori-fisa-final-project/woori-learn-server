package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointsExchangeResponseDto {
    private Long requestId;
    private Long userId;
    private Integer exchangeAmount;
    private PointsStatus status;
    private String requestDate;
    private String message;
    private LocalDateTime processedDate;

}
