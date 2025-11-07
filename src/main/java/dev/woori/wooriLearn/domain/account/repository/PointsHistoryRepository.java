package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {

    List<PointsHistory> findAllByUser_Id(Long userId);

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


}
