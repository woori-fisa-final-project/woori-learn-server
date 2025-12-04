package dev.woori.wooriLearn.domain.account.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


public record PointsDepositRequestDto(
        @NotNull @Positive Integer amount,
        String reason
) {}
