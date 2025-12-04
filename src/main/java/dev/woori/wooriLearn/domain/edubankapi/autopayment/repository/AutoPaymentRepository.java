package dev.woori.wooriLearn.domain.edubankapi.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // 페이징 지원 메서드
    @EntityGraph(attributePaths = {"educationalAccount"})
    Page<AutoPayment> findByEducationalAccountId(Long educationalAccountId, Pageable pageable);

    @EntityGraph(attributePaths = {"educationalAccount"})
    Page<AutoPayment> findByEducationalAccountIdAndProcessingStatus(
            Long educationalAccountId,
            AutoPaymentStatus processingStatus,
            Pageable pageable);

    /**
     * 자동이체 정보를 교육용 계좌 및 사용자 정보와 함께 조회 (N+1 문제 해결)
     * @param id 자동이체 ID
     * @return 교육용 계좌와 사용자 정보가 fetch된 AutoPayment
     */
    @Query("SELECT ap FROM AutoPayment ap " +
           "JOIN FETCH ap.educationalAccount ea " +
           "JOIN FETCH ea.user " +
           "WHERE ap.id = :id")
    Optional<AutoPayment> findByIdWithAccountAndUser(@Param("id") Long id);
}