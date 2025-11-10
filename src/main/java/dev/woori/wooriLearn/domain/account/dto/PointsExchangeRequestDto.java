package dev.woori.wooriLearn.domain.account.dto;

public record PointsExchangeRequestDto(
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Integer exchangeAmount,
        @jakarta.validation.constraints.NotBlank String accountNum,
        @jakarta.validation.constraints.NotBlank String bankCode
) {}
