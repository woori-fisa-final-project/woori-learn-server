package dev.woori.wooriLearn.domain.autopayment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;

import java.time.LocalDate;

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
) {

    public static AutoPaymentResponse of(AutoPayment autoPayment) {
        return new AutoPaymentResponse(
                autoPayment.getId(),
                autoPayment.getEducationalAccount().getId(),
                autoPayment.getDepositNumber(),
                autoPayment.getDepositBankCode(),
                autoPayment.getAmount(),
                autoPayment.getCounterpartyName(),
                autoPayment.getDisplayName(),
                autoPayment.getTransferCycle(),
                autoPayment.getDesignatedDate(),
                autoPayment.getStartDate(),
                autoPayment.getExpirationDate(),
                autoPayment.getProcessingStatus().getDescription()
        );
    }
}