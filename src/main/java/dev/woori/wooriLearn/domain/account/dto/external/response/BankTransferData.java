package dev.woori.wooriLearn.domain.account.dto.external.response;

public record BankTransferData(
        String fromAccount,
        String toAccount,
        Long amount
) {}