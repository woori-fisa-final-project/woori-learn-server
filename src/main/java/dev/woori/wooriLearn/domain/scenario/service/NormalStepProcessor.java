package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NormalStepProcessor implements StepProcessor {

    @Override
    public AdvanceResDto process(StepContext ctx, ScenarioProgressService service) {
        var user = ctx.user();
        Scenario scenario = ctx.scenario();
        Map<Long, ScenarioStep> byId = ctx.stepsById();
        ScenarioStep current = ctx.current();
        ScenarioProgress progress = ctx.progress();

        ScenarioStep next = (current.getNextStep() != null)
                ? byId.get(current.getNextStep().getId())
                : null;

        if (next == null) {
            // 정루트 상의 마지막 → 완료
            service.ensureCompletedOnce(user, scenario);
            double rate = service.monotonicRate(progress, 100.0);

            Long startId = service.inferStartStepId(byId);
            ScenarioStep start = byId.get(startId);
            if (start == null) {
                throw new CommonException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenario.getId()
                );
            }

            progress.moveToStep(start, rate);
            service.saveProgress(progress);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        service.updateProgressAndSave(progress, next, scenario, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, service.mapStep(next), null);
    }
}
