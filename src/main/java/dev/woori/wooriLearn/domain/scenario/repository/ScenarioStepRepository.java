package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
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

    /** 중복 제거용: 시작 스텝을 반환(없으면 최소 id 스텝, 그래도 없으면 예외) */
    default ScenarioStep findStartStepOrFail(Long scenarioId) {
        return findStartStep(scenarioId)
                .orElseGet(() -> findFirstByScenarioIdOrderByIdAsc(scenarioId)
                        .orElseThrow(() ->
                                new CommonException(
                                        ErrorCode.INTERNAL_SERVER_ERROR,
                                        "시작 스텝을 찾을 수 없습니다. scenarioId=" + scenarioId
                                )));
    }
}
