package dev.woori.wooriLearn.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PointsExchangeRequestDto {



    private Integer exchangeAmount;
    private String accountNum;
    private String bankcode;
}
