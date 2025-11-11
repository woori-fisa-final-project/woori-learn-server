package dev.woori.wooriLearn.domain.scenario.dto;

public record ProgressSaveResDto(
        Long scenarioId,
        Long nowStepId,
        Double progressRate // 0.0 ~ 100.0
) {}