package dev.woori.wooriLearn.domain.account.dto.external.response;

public record BankTransferResDto(
        String fromAccount,
        String toAccount,
        long amount,
        String message,
        int code,
        boolean success
) {}
