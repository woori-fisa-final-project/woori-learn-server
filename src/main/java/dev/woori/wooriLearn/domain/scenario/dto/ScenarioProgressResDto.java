package dev.woori.wooriLearn.domain.scenario.dto;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;

public record ScenarioProgressResDto(
        Long scenarioId,
        String title,
        Double progressRate
) {
    public static ScenarioProgressResDto from(ScenarioProgress entity) {
        return new ScenarioProgressResDto(
                entity.getScenario().getId(),
                entity.getScenario().getTitle(),
                entity.getProgressRate()
        );
    }
}