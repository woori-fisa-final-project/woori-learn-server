package dev.woori.wooriLearn.domain.scenario.dto;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;

import java.time.LocalDateTime;

public record ScenarioCompletedResDto(
        Long scenarioId,
        String title,
        LocalDateTime completedAt
) {
    public static ScenarioCompletedResDto from(ScenarioCompleted entity) {
        return new ScenarioCompletedResDto(
                entity.getScenario().getId(),
                entity.getScenario().getTitle(),
                entity.getCompletedAt()
        );
    }
}