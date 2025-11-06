package dev.woori.wooriLearn.domain.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AutoPaymentRepository extends JpaRepository<AutoPayment, Long> {

        @Query("SELECT ap FROM AutoPayment ap " +
                "JOIN FETCH ap.educationalAccount " +
                "WHERE ap.educationalAccount.id = :educationalAccountId")
        List<AutoPayment> findByEducationalAccountId(
                @Param("educationalAccountId") Long educationalAccountId);

        @Query("SELECT ap FROM AutoPayment ap " +
                "JOIN FETCH ap.educationalAccount " +
                "WHERE ap.educationalAccount.id = :educationalAccountId " +
                "AND ap.processingStatus = :status")
        List<AutoPayment> findByEducationalAccountIdAndProcessingStatus(
                @Param("educationalAccountId") Long educationalAccountId,
                @Param("status") AutoPaymentStatus processingStatus);

}
