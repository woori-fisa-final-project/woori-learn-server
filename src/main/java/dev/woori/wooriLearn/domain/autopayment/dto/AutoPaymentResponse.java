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
    /**
     * AutoPayment 엔티티로부터 Response DTO 생성
     *
     * @param autoPayment 자동이체 엔티티
     * @param educationalAccountId 교육계좌 ID (명시적 전달로 N+1 문제 방지)
     * @return AutoPaymentResponse
     */
    public static AutoPaymentResponse of(AutoPayment autoPayment, Long educationalAccountId) {
        return new AutoPaymentResponse(
                autoPayment.getId(),
                educationalAccountId,
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