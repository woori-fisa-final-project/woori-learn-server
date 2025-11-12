package dev.woori.wooriLearn.domain.scenario.dto;

/**
 * 시나리오 진행(체크포인트) 저장 응답 DTO
 * @param scenarioId    진행 중인 시나리오 ID
 * @param nowStepId     현재 저장된(재개 지점) 스텝 ID
 * @param progressRate  진행률(%), 0.0 ~ 100.0
 */
public record ProgressSaveResDto(
        Long scenarioId,
        Long nowStepId,
        Double progressRate // 0.0 ~ 100.0
) {}