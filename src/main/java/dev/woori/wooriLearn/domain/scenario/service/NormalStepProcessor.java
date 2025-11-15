package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 일반(CHOICE/퀴즈/배드 브랜치가 아닌) 스텝을 처리하는 Processor
 */
@Component
public class NormalStepProcessor implements StepProcessor {

    @Override
    public AdvanceResDto process(StepContext ctx, ScenarioProgressService service) {
        Scenario scenario = ctx.scenario();         // 진행률 계산 및 완료 이력에 사용
        Map<Long, ScenarioStep> byId = ctx.byId();  // stepId -> ScenarioStep 맵
        ScenarioStep current = ctx.current();       // 현재 스텝
        ScenarioProgress progress = ctx.progress(); // 사용자 진행 엔티티

        // 1) 현재 스텝이 가리키는 nextStep을 ID 기준으로 맵에서 조회
        ScenarioStep next = (current.getNextStep() != null)
                ? byId.get(current.getNextStep().getId())
                : null;

        // 2) 다음 스텝이 없다면 정상 루트의 마지막 스텝으로 보고 완료 처리
        if (next == null) {
            return service.handleScenarioCompletion(ctx);
        }

        // 3) 다음 스텝이 있다면 진행률을 갱신하면서 해당 스텝으로 이동
        service.updateProgressAndSave(progress, next, scenario, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, service.mapStep(next), null);
    }
}
