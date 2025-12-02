package dev.woori.wooriLearn.domain.account.dto.external.request;

public record BankTransferReqDto(
        String fromAccount,
        String toAccount,
        Long amount
) {}