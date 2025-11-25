package dev.woori.wooriLearn.domain.account.dto;

public record BankTokenResDto(
        String code,
        String message,
        TokenData data
) {
}
