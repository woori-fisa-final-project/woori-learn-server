package dev.woori.wooriLearn.domain.edubankapi.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoPaymentRepository extends JpaRepository<AutoPayment, Long> {

    @EntityGraph(attributePaths = {"educationalAccount"})
    List<AutoPayment> findByEducationalAccountId(Long educationalAccountId);

    @EntityGraph(attributePaths = {"educationalAccount"})
    List<AutoPayment> findByEducationalAccountIdAndProcessingStatus(
            Long educationalAccountId,
            AutoPaymentStatus processingStatus);

    @EntityGraph(attributePaths = {"educationalAccount"})
    Optional<AutoPayment> findById(Long id);

    boolean existsByIdAndEducationalAccountId(Long id, Long educationalAccountId);
}