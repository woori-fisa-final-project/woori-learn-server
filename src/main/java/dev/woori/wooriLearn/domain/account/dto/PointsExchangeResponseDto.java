package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsExchangeStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointsExchangeResponseDto {
    private Long requestId;
    private Long user_id;
    private Integer exchangeAmount;
    private PointsExchangeStatus status;
    private String requestDate;
    private String message;
}
