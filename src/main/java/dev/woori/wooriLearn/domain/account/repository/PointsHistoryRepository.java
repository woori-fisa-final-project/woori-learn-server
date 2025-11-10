package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {



    @Query("SELECT p FROM PointsHistory p WHERE "
            + "p.user.id = :userId "
            + "AND p.type = :type " // type 조건 추가
            + "AND (:status IS NULL OR p.status = :status) "
            + "AND (:start IS NULL OR p.createdAt >= :start) "
            + "AND (:end IS NULL OR p.createdAt <= :end)")
    List<PointsHistory> findByFilters(
            @Param("userId") Long userId,
            @Param("type") PointsHistoryType type, // 파라미터 추가
            @Param("status") PointsStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Sort sort
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM PointsHistory h WHERE h.id = :id")
    Optional<PointsHistory> findAndLockById(@Param("id") Long id);

}
