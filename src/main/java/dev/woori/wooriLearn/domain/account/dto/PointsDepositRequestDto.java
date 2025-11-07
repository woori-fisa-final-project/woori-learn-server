package dev.woori.wooriLearn.domain.account.dto;

public record PointsDepositRequestDto(
        Long userId,
        Integer amount,
        String reason
) {}
