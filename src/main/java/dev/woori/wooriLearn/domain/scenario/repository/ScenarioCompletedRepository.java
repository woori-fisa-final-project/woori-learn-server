package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScenarioCompletedRepository extends JpaRepository<ScenarioCompleted, Long> {

    boolean existsByUserAndScenario(Users user, Scenario scenario);

    @EntityGraph(attributePaths = "scenario")
    List<ScenarioCompleted> findByUser(Users user);

    long countByUser(Users user);

    @Query("""
        SELECT sc.user.id AS userId, COUNT(sc) AS completedCount
        FROM ScenarioCompleted sc
        WHERE sc.user.id IN :userIds
        GROUP BY sc.user.id
    """)
    List<ScenarioCompletedCount> countCompletedByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "INSERT IGNORE INTO scenario_completed (user_id, scenario_id, completed_at) VALUES (:userId, :scenarioId, NOW(6))", nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId, @Param("scenarioId") Long scenarioId);
}
