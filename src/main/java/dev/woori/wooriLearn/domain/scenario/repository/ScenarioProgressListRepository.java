package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgressList;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface ScenarioProgressListRepository extends JpaRepository<ScenarioProgressList, Long> {
    Optional<ScenarioProgressList> findByUserAndScenario(Users user, Scenario scenario);
    @Modifying
    void deleteByUserAndScenario(Users user, Scenario scenario);
}
