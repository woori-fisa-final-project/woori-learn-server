package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;

import java.time.LocalDateTime;

public record EdubankapiTransactionHistoryDto(
        LocalDateTime transactionDate,
        String counterpartyName,
        String displayName,
        Integer amount,
        String description
) {
    public static EdubankapiTransactionHistoryDto from(TransactionHistory entity) {
        return new EdubankapiTransactionHistoryDto(
                entity.getTransactionDate(),
                entity.getCounterpartyName(),
                entity.getDisplayName(),
                entity.getAmount(),
                entity.getDescription()
        );
    }
}
