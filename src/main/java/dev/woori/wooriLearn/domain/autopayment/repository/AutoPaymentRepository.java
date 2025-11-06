package dev.woori.wooriLearn.domain.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoPaymentRepository extends JpaRepository<AutoPayment, Long> {

    @EntityGraph(attributePaths = {"educationalAccount"})
    List<AutoPayment> findByEducationalAccountId(Long educationalAccountId);

    @EntityGraph(attributePaths = {"educationalAccount"})
    List<AutoPayment> findByEducationalAccountIdAndProcessingStatus(
            Long educationalAccountId,
            AutoPaymentStatus processingStatus);
}