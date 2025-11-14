package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.user.entity.Users;

import java.util.Map;

public record StepContext(
        Users user,
        Scenario scenario,
        ScenarioStep current,
        Integer answer,
        Map<Long, ScenarioStep> stepsById,
        ScenarioProgress progress,
        boolean badBranch,
        boolean badEnding
) {}
