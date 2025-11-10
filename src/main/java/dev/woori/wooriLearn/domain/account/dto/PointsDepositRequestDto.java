package dev.woori.wooriLearn.domain.account.dto;

public record PointsDepositRequestDto(
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Integer amount,
        String reason
) {}
