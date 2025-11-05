package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.PointsExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsExchangeRequestRepository extends JpaRepository<PointsExchangeRequest, Long> {
    List<PointsExchangeRequest> findAllByUserId(Long userId);
}
