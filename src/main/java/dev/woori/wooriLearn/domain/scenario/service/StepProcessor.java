package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;

public interface StepProcessor {
    AdvanceResDto process(StepContext ctx, ScenarioProgressService service);
}
