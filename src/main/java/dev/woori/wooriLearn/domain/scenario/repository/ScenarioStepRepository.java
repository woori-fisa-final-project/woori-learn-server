package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScenarioStepRepository extends JpaRepository<ScenarioStep, Long> {
    List<ScenarioStep> findByScenarioId(Long scenarioId);
    Optional<ScenarioStep> findFirstByScenarioIdOrderByIdAsc(Long scenarioId);

    // 시작 스텝(다른 스텝의 next로 참조되지 않는 스텝)
    @Query("""
      select s from ScenarioStep s
      where s.scenario.id = :scenarioId
        and s.id not in (
          select ss.nextStep.id from ScenarioStep ss
          where ss.scenario.id = :scenarioId and ss.nextStep is not null
        )
      order by s.id asc
    """)
    Optional<ScenarioStep> findStartStep(Long scenarioId);
}
