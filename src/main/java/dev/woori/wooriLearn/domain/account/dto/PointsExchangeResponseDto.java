package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.WithdrawStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawResponseDto {
    private Long requestId;
    private Long user_id;
    private Integer withdrawAmount;
    private WithdrawStatus status;
    private String requestDate;
    private String message;
}
