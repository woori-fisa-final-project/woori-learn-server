package dev.woori.wooriLearn.domain.scenario.service;

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
        Map<Long, ScenarioStep> byId = ctx.byId();
        ScenarioStep current = ctx.current();
        ScenarioProgress progress = ctx.progress();

        ScenarioStep next = (current.getNextStep() != null)
                ? byId.get(current.getNextStep().getId())
                : null;

        if (next == null) {
            return service.handleScenarioCompletion(ctx);
        }

        service.updateProgressAndSave(progress, next, scenario, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, service.mapStep(next), null);
    }
}
