package dev.woori.wooriLearn.domain.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoPaymentRepository extends JpaRepository<AutoPayment, Long> {

    /**
     * 교육용 계좌로 자동이체 목록 조회
     */
    List<AutoPayment> findByEducationalAccount(EducationalAccount educationalAccount);

    /**
     * 교육용 계좌 ID로 자동이체 목록 조회
     */
    List<AutoPayment> findByEducationalAccountId(Long educationalAccountId);

    /**
     * 교육용 계좌 ID와 상태로 자동이체 목록 조회
     */
    List<AutoPayment> findByEducationalAccountIdAndProcessingStatus(
            Long educationalAccountId,
            AutoPaymentStatus processingStatus
    );

    /**
     * 자동이체 ID와 교육용 계좌 ID로 조회 (본인 확인용)
     */
    Optional<AutoPayment> findByIdAndEducationalAccountId(
            Long id,
            Long educationalAccountId
    );
}