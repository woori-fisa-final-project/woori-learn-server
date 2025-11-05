package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.PointsExchangeStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {
    List<PointsHistory> findAllByUserId(Long userId);
    @Query("SELECT p FROM PointsHistory p WHERE p.user.id = :userId "
            + "AND (:status = 'ALL' OR p.status = :status) "
            + "AND (:start IS NULL OR p.paymentDate >= :start) "
            + "AND (:end IS NULL OR p.paymentDate <= :end)")
    List<PointsHistory> findByFilters(
            @Param("userId") Long userId,
            @Param("status") PointsExchangeStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
