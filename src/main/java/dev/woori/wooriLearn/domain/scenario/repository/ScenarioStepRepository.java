package dev.woori.wooriLearn.domain.scenario.repository;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 시나리오의 개별 스텝(ScenarioStep) 조회용
 *
 * - nextStep이 LAZY 로딩인 점을 고려하여 N+1을 회피하기 위한 JOIN FETCH 메서드 제공
 */
public interface ScenarioStepRepository extends JpaRepository<ScenarioStep, Long> {
    Optional<ScenarioStep> findFirstByScenarioIdOrderByIdAsc(Long scenarioId);

    /**
     * 특정 시나리오의 스텝들을 nextStep까지 한 번에 로딩
     * - LAZY 로딩으로 인한 N+1 문제를 회피하기 위해 사용
     * - JOIN FETCH 시 중복 로우가 발생할 수 있으므로 distinct로 제거
     *
     * @param scenarioId    시나리오 ID
     * @return nextStep이 JOIN FETCH된 스텝 목록
     */
    @Query("""
        select distinct s
        from ScenarioStep s
        left join fetch s.nextStep
        where s.scenario.id = :scenarioId
    """)
    List<ScenarioStep> findByScenarioIdWithNextStep(Long scenarioId);

    /**
     * 시작 스텝 조회
     * - 다른 스텝의 nextStep으로 참조되지 않는 스텝을 시작 스텝으로 간주
     * - 다수 후보가 존재하면 ID 오름차순 정렬을 통해 가장 작은 것 선택
     *
     * @param scenarioId    시나리오 ID
     * @return 시작 스텝
     */
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
