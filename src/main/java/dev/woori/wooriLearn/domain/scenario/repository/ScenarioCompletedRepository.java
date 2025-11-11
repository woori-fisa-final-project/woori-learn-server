package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioCompletedRepository extends JpaRepository<ScenarioCompleted, Long> {
    Optional<ScenarioCompleted> findByUserAndScenario(Users user, Scenario scenario);
    boolean existsByUserAndScenario(Users user, Scenario scenario);
}
