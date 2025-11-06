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

        // status 파라미터 처리
        if (StringUtils.hasText(status)) {
            if (!ALL_STATUS.equalsIgnoreCase(status)) {
                // "ALL"이 아닌 경우 해당 상태로 필터링
                AutoPaymentStatus paymentStatus = resolveStatus(status);
                spec = spec.and(AutoPaymentSpecification.hasStatus(paymentStatus));
            }
            // "ALL"인 경우 상태 필터링을 추가하지 않음
        } else {
            // status 파라미터가 없는 경우 기본적으로 ACTIVE 상태만 조회
            spec = spec.and(AutoPaymentSpecification.hasStatus(AutoPaymentStatus.ACTIVE));
        }
        // "ALL"인 경우 상태 조건을 추가하지 않음 (전체 조회)

        // 동적 쿼리 실행
        List<AutoPayment> autoPayments = autoPaymentRepository.findAll(spec);

        // educationalAccountId를 명시적으로 전달하여 fetch join 의존성 제거
        return autoPayments.stream()
                .map(autoPayment -> AutoPaymentResponse.of(autoPayment, educationalAccountId))
                .toList();
    }

    /**
     * status 문자열을 AutoPaymentStatus enum으로 변환
     * - 유효한 enum 값으로 변환
     * - 유효하지 않은 값인 경우 예외 발생
     *
     * @param status 변환할 상태 문자열 (null이 아니고 빈 문자열이 아님을 보장)
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