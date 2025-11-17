package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScenarioCompletedRepository extends JpaRepository<ScenarioCompleted, Long> {
    boolean existsByUserAndScenario(Users user, Scenario scenario);

    @Query("SELECT sc FROM ScenarioCompleted sc JOIN FETCH sc.scenario WHERE sc.user = :user")
    List<ScenarioCompleted> findByUser(@Param("user") Users user);
}
