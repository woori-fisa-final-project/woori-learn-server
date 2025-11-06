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
     * @param educationalAccountId 교육계좌 ID (명시적 전달로 fetch join 의존성 제거)
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

    /**
     * AutoPayment 엔티티로부터 Response DTO 생성 (편의 메소드)
     * 주의: educationalAccount가 fetch join으로 로드되어 있어야 합니다.
     *
     * @param autoPayment 자동이체 엔티티 (educationalAccount가 로드된 상태)
     * @return AutoPaymentResponse
     */
    public static AutoPaymentResponse of(AutoPayment autoPayment) {
        return of(autoPayment, autoPayment.getEducationalAccount().getId());
    }
}