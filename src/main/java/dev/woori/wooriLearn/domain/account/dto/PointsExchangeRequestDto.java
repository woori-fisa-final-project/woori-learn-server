package dev.woori.wooriLearn.domain.account.dto;

import lombok.Getter;

@Getter
public class PointsExchangeRequestDto {
    private Long db_id;
    // 또는 user_id
    private Integer exchangeAmount;
    private String accountNum;
    private String bankcode;
}
