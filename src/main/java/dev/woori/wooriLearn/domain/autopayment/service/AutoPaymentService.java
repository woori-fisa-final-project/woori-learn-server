package dev.woori.wooriLearn.domain.autopayment.service;


import dev.woori.wooriLearn.domain.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.autopayment.repository.AutoPaymentRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AutoPaymentService {

    private final AutoPaymentRepository autoPaymentRepository;

    public List<AutoPaymentResponse> getAutoPaymentList(Long educationalAccountId, String status) {
        List<AutoPayment> autoPayments;

        if (status != null && !status.isEmpty()) {
            // 상태 필터링
            AutoPaymentStatus paymentStatus = AutoPaymentStatus.valueOf(status.toUpperCase());
            autoPayments = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                    educationalAccountId, paymentStatus);
        } else {
            // 전체 조회
            autoPayments = autoPaymentRepository.findByEducationalAccountId(educationalAccountId);
        }

        return autoPayments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private AutoPaymentResponse convertToResponse(AutoPayment autoPayment) {
        return AutoPaymentResponse.builder()
                .id(autoPayment.getId())
                .educationalAccountId(autoPayment.getEducationalAccount().getId())
                .depositNumber(autoPayment.getDepositNumber())
                .depositBankCode(autoPayment.getDepositBankCode())
                .amount(autoPayment.getAmount())
                .counterpartyName(autoPayment.getCounterpartyName())
                .displayName(autoPayment.getDisplayName())
                .transferCycle(autoPayment.getTransferCycle())
                .designatedDate(autoPayment.getDesignatedDate())
                .startDate(autoPayment.getStartDate())
                .expirationDate(autoPayment.getExpirationDate())
                .processingStatus(autoPayment.getProcessingStatus().name())
                .build();
    }
}