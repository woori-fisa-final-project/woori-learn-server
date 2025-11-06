package dev.woori.wooriLearn.domain.autopayment.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.autopayment.repository.AutoPaymentRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AutoPaymentService {

    private final AutoPaymentRepository autoPaymentRepository;

    public List<AutoPaymentResponse> getAutoPaymentList(Long educationalAccountId, String status) {
        // "ALL"인 경우 전체 조회
        if ("ALL".equalsIgnoreCase(status)) {
            List<AutoPayment> autoPayments = autoPaymentRepository.findByEducationalAccountId(educationalAccountId);
            return autoPayments.stream()
                    .map(AutoPaymentResponse::of)
                    .toList();
        }

        // 그 외의 경우 상태별 조회 (null이면 ACTIVE)
        AutoPaymentStatus paymentStatus = resolveStatus(status);
        List<AutoPayment> autoPayments = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                educationalAccountId, paymentStatus);

        return autoPayments.stream()
                .map(AutoPaymentResponse::of)
                .toList();
    }

    private AutoPaymentStatus resolveStatus(String status) {
        // status가 없으면 기본값 ACTIVE
        if (!StringUtils.hasText(status)) {
            return AutoPaymentStatus.ACTIVE;
        }

        // 유효한 enum 값으로 변환
        try {
            return AutoPaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 상태 값입니다. (사용 가능: ACTIVE, CANCELLED, ALL)");
        }
    }
}