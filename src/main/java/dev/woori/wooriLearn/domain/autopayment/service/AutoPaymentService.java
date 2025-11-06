package dev.woori.wooriLearn.domain.autopayment.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.autopayment.repository.AutoPaymentRepository;
import dev.woori.wooriLearn.domain.autopayment.repository.AutoPaymentSpecification;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
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
    private static final String ALL_STATUS = "ALL";

    public List<AutoPaymentResponse> getAutoPaymentList(Long educationalAccountId, String status) {

        Specification<AutoPayment> spec = AutoPaymentSpecification.hasEducationalAccountId(educationalAccountId);

        if (!StringUtils.hasText(status)) {
            spec = spec.and(AutoPaymentSpecification.hasStatus(AutoPaymentStatus.ACTIVE));
        } else if (!ALL_STATUS.equalsIgnoreCase(status)) {
            // "ALL"이 아닌 경우 해당 상태로 필터링
            AutoPaymentStatus paymentStatus = resolveStatus(status);
            spec = spec.and(AutoPaymentSpecification.hasStatus(paymentStatus));
        }
        // 동적 쿼리 실행
        List<AutoPayment> autoPayments = autoPaymentRepository.findAll(spec);

        return autoPayments.stream()
                .map(AutoPaymentResponse::of)
                .toList();
    }
    private AutoPaymentStatus resolveStatus(String status) {
        try {
            return AutoPaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 상태 값입니다. (사용 가능: " + AutoPaymentStatus.getAvailableValues() + ")");
        }
    }
}