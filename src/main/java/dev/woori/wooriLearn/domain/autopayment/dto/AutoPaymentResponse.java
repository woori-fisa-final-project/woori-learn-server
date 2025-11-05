package dev.woori.wooriLearn.domain.autopayment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;

import java.time.LocalDate;

/**
 * 자동이체 응답 DTO
 */
public record AutoPaymentResponse(
        Long id,
        Long educationalAccountId,
        String depositNumber,
        String depositBankCode,
        Integer amount,
        String counterpartyName,
        String displayName,
        Integer transferCycle,
        Integer designatedDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate expirationDate,
        String processingStatus
){
    public static AutoPaymentResponse of(AutoPayment a) {
        return new AutoPaymentResponse(
                a.getId(),
                a.getEducationalAccount().getId(),
                a.getDepositNumber(),
                a.getDepositBankCode(),
                a.getAmount(),
                a.getCounterpartyName(),
                a.getDisplayName(),
                a.getTransferCycle(),
                a.getDesignatedDate(),
                a.getStartDate(),
                a.getExpirationDate(),
                a.getProcessingStatus().name()
        );
    }
}