package dev.woori.wooriLearn.domain.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@NotNull
@Positive
public record PointsDepositRequestDto(
          Integer amount,
        String reason
) {}
