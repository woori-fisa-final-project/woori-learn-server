package dev.woori.wooriLearn.domain.edubankapi.autopayment.cache;

import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.repository.AutoPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 자동이체 캐싱 전용 컴포넌트
 *
 * Self-Invocation 문제 해결을 위해 캐싱 로직을 별도 컴포넌트로 분리
 * - AutoPaymentService 내부에서 캐싱 메서드를 호출하면 프록시를 거치지 않아 @Cacheable이 동작하지 않음
 * - 별도 컴포넌트로 분리하면 Spring AOP 프록시를 통해 호출되어 캐싱이 정상 동작
 *
 * 책임:
 * - 자동이체 목록 조회 시 캐싱 처리
 * - 자동이체 상세 조회 시 캐싱 처리
 * - 캐시 키 생성 및 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPaymentCacheManager {

    private final AutoPaymentRepository autoPaymentRepository;

    private static final String ALL_STATUS = "ALL";

    /**
     * 자동이체 목록 조회 (캐시 적용)
     *
     * 정규화된 status를 사용하여 캐시 키 중복 방지
     * Note: 호출 전에 AutoPaymentService에서 유효성 검증 및 권한 체크 완료
     *
     * @param educationalAccountId 교육용 계좌 ID
     * @param normalizedStatus 정규화된 상태 ("ACTIVE", "CANCELLED", "ALL")
     * @param currentUserId 현재 사용자 ID (캐시 키용)
     * @return 자동이체 응답 목록
     */
    @Cacheable(value = "autoPaymentList",
               key = "#currentUserId + ':' + #educationalAccountId + ':' + #normalizedStatus")
    public List<AutoPaymentResponse> getAutoPaymentListCached(
            Long educationalAccountId,
            String normalizedStatus,
            String currentUserId) {

        log.info("자동이체 목록 조회 (캐시 미스) - 계좌ID: {}, 상태: {}, 사용자: {}",
                educationalAccountId, normalizedStatus, currentUserId);

        List<AutoPayment> autoPayments;

        if (ALL_STATUS.equalsIgnoreCase(normalizedStatus)) {
            autoPayments = autoPaymentRepository.findByEducationalAccountId(educationalAccountId);
        } else {
            // normalizedStatus는 이미 유효성 검증되어 "ACTIVE" 또는 "CANCELLED"만 들어옴
            AutoPaymentStatus paymentStatus = AutoPaymentStatus.valueOf(normalizedStatus);
            autoPayments = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                    educationalAccountId, paymentStatus);
        }

        return autoPayments.stream()
                .map(autoPayment -> AutoPaymentResponse.of(autoPayment, educationalAccountId))
                .toList();
    }

    /**
     * 자동이체 상세 조회 (캐시 적용)
     *
     * AutoPayment 엔티티에서 필요한 정보를 직접 추출하여 사용
     * - autoPaymentId: autoPayment.getId()
     * - educationalAccountId: autoPayment.getEducationalAccount().getId()
     *
     * @param currentUserId 현재 사용자 ID (캐시 키용)
     * @param autoPayment 자동이체 엔티티 (이미 조회 및 권한 검증 완료)
     * @return 자동이체 응답 DTO
     */
    @Cacheable(value = "autoPaymentDetail",
               key = "#currentUserId + ':' + #autoPayment.id",
               unless = "#result == null")
    public AutoPaymentResponse getAutoPaymentDetailCached(
            String currentUserId,
            AutoPayment autoPayment) {

        Long autoPaymentId = autoPayment.getId();
        Long educationalAccountId = autoPayment.getEducationalAccount().getId();

        log.info("자동이체 상세 조회 (캐시 미스) - ID: {}, 사용자: {}", autoPaymentId, currentUserId);
        return AutoPaymentResponse.of(autoPayment, educationalAccountId);
    }
}
