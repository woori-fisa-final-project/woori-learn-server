package dev.woori.wooriLearn.domain.account.dto;

public record PointsExchangeRequestDto(
        Integer exchangeAmount,
        String accountNum,
        String bankCode
) {}
