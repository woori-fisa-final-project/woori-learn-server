package dev.woori.wooriLearn.domain.edubankapi.autopayment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;

import java.time.LocalDate;

public record AutoPaymentResponse(
        Long id,
        Long educationalAccountId,
        String accountNumber,
        String bankName,
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
    public static AutoPaymentResponse of(AutoPayment autoPayment, Long educationalAccountId) {
        return new AutoPaymentResponse(
                autoPayment.getId(),
                educationalAccountId,
                autoPayment.getEducationalAccount().getAccountNumber(),
                "우리은행", // 교육용 계좌는 모두 우리은행
                autoPayment.getDepositNumber(),
                autoPayment.getDepositBankCode(),
                autoPayment.getAmount(),
                autoPayment.getCounterpartyName(),
                autoPayment.getDisplayName(),
                autoPayment.getTransferCycle(),
                autoPayment.getDesignatedDate(),
                autoPayment.getStartDate(),
                autoPayment.getExpirationDate(),
                autoPayment.getProcessingStatus().name()
        );
    }
}