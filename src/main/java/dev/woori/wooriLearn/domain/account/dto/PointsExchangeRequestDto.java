package dev.woori.wooriLearn.domain.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointsExchangeRequestDto(
        @NotNull @Positive Integer exchangeAmount,
        @NotBlank String accountNum,
        @NotBlank String bankCode

) {}
