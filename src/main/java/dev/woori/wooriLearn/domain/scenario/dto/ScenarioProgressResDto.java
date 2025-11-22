package dev.woori.wooriLearn.domain.scenario.dto;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;

/**
 * 사용자가 진행 중인 시나리오의 진행률 정보를 내려주는 응답 DTO
 * @param scenarioId    진행 중인 시나리오 ID
 * @param title         진행 중인 시나리오 제목
 * @param progressRate  시나리오 진행률 (0.0 ~ 100.0)
 */
public record ScenarioProgressResDto(
        Long scenarioId,
        String title,
        Double progressRate
) {
    /**
     * ScenarioProgress 엔티티에서 응답 DTO로 변환하는 정적 팩토리 메서드
     * @param entity    시나리오 진행 상태 엔티티
     * @return ScenarioProgressResDto 응답 객체
     */
    public static ScenarioProgressResDto from(ScenarioProgress entity) {
        return new ScenarioProgressResDto(
                entity.getScenario().getId(),
                entity.getScenario().getTitle(),
                entity.getProgressRate()
        );
    }
}