package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioProgressRepository extends JpaRepository<ScenarioProgress, Long> {
    Optional<ScenarioProgress> findByUserAndScenario(Users user, Scenario scenario);

    List<ScenarioProgress> findByUser(Users user);
}
