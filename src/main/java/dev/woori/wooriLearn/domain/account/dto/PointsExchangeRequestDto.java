package dev.woori.wooriLearn.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PointsExchangeRequestDto {

    @JsonProperty("db_id")   // JSON → Java 매핑 유지
    private Long dbId;


    private Integer exchangeAmount;
    private String accountNum;
    private String bankcode;
}
