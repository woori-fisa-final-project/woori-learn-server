package dev.woori.wooriLearn.domain.account.dto;

public record PointsDepositRequestDto(
        Integer amount,
        String reason
) {}
