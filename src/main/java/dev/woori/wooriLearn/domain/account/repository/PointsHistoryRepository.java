package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM PointsHistory h JOIN FETCH h.user WHERE h.id = :id")
    Optional<PointsHistory> findAndLockById(@Param("id") Long id);

    // 단순 타입 + 상태 조회 (프론트 사용자 내역용)
    @EntityGraph(attributePaths = {"user"})
    Page<PointsHistory> findByTypeAndStatus(PointsHistoryType type, PointsStatus status, Pageable pageable);

    List<PointsHistory> findByUserId(Long userId);
}
