package dev.woori.wooriLearn.domain.account.dto;

public record AccountCreateResDto(
        String code,
        String message,
        AccountInfo data
) {
}
