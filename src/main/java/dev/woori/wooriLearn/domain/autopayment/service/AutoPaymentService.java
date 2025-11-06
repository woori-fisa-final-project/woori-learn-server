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

        if (StringUtils.hasText(status) && !ALL_STATUS.equalsIgnoreCase(status)) {
            // status 값이 있고 'ALL'이 아닌 경우, 해당 상태로 필터링합니다.
            spec = spec.and(AutoPaymentSpecification.hasStatus(resolveStatus(status)));
        } else if (!StringUtils.hasText(status)) {
            // status 값이 없는 경우(null 또는 empty), 기본적으로 'ACTIVE' 상태로 필터링합니다.
            spec = spec.and(AutoPaymentSpecification.hasStatus(AutoPaymentStatus.ACTIVE));
        }

        List<AutoPayment> autoPayments = autoPaymentRepository.findAll(spec);

        return autoPayments.stream()
                .map(AutoPaymentResponse::of)
                .toList();
    }
    /**
     * status 문자열을 AutoPaymentStatus enum으로 변환
     *
     * @param status 변환할 상태 문자열
     * @return AutoPaymentStatus enum
     * @throws CommonException 유효하지 않은 상태 값인 경우
     */
    private AutoPaymentStatus resolveStatus(String status) {
        try {
            return AutoPaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 상태 값입니다. (사용 가능: " + AutoPaymentStatus.getAvailableValues() + ")");
        }
    }
}