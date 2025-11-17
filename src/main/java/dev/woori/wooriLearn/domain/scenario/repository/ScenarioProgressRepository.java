package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioProgressRepository extends JpaRepository<ScenarioProgress, Long> {
    Optional<ScenarioProgress> findByUserAndScenario(Users user, Scenario scenario);

    @Query("SELECT sp FROM ScenarioProgress sp JOIN FETCH sp.scenario WHERE sp.user = :user")
    List<ScenarioProgress> findByUser(@Param("user") Users user);
}
