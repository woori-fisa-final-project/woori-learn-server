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
        // 기본 조건: educationalAccountId로 필터링
        Specification<AutoPayment> spec = AutoPaymentSpecification.hasEducationalAccountId(educationalAccountId);

        // status가 "ALL"이 아니면 상태 조건 추가
        if (!ALL_STATUS.equalsIgnoreCase(status)) {
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
        // status가 없으면 기본값 ACTIVE
        if (!StringUtils.hasText(status)) {
            return AutoPaymentStatus.ACTIVE;
        }

        // 유효한 enum 값으로 변환
        try {
            return AutoPaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 상태 값입니다. (사용 가능: " + java.util.Arrays.toString(AutoPaymentStatus.values()) + ")");
        }
    }
}