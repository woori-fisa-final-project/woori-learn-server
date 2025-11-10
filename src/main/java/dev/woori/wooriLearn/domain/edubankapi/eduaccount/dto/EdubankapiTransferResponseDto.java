package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EdubankapiTransferResponseDto(
        String transactionId,
        LocalDateTime transactionDate,
        String counterpartyName,
        Integer amount,
        Integer balance,
        String message
) {
}
