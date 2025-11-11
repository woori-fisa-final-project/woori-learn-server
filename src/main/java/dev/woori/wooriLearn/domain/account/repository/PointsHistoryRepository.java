package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {



    @Query("SELECT p FROM PointsHistory p JOIN FETCH p.user WHERE "
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
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "2000"),
            @QueryHint(name = "org.hibernate.timeout", value = "3")
    })
    @Query("SELECT h FROM PointsHistory h JOIN FETCH h.user WHERE h.id = :id")
    Optional<PointsHistory> findAndLockById(@Param("id") Long id);

    /**
     * 관리자 전체 조회(페이징). 특정 사용자 필터, 상태, 기간, 타입 조건 적용.
     * Page 카운트 쿼리와 충돌을 피하기 위해 fetch join은 사용하지 않음.
     */
    @Query("SELECT p FROM PointsHistory p WHERE "
            + "p.type = :type "
            + "AND (:userId IS NULL OR p.user.id = :userId) "
            + "AND (:status IS NULL OR p.status = :status) "
            + "AND (:start IS NULL OR p.createdAt >= :start) "
            + "AND (:end IS NULL OR p.createdAt <= :end)")
    Page<PointsHistory> findAllByFilters(
            @Param("userId") Long userId,
            @Param("type") PointsHistoryType type,
            @Param("status") PointsStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

}
