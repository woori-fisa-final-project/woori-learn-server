package dev.woori.wooriLearn.domain.account.dto.external.response;

public record BankTransferResDto(
        int code,
        boolean success,
        String message,
        BankTransferData data
) {}
