package dev.woori.wooriLearn.domain.scenario.dto;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;

import java.time.LocalDateTime;

/**
 * 사용자가 완료한 시나리오 정보를 내려주는 응답 DTO
 * @param scenarioId    완료한 시나리오 ID
 * @param title         완료한 시나리오 제목
 * @param completedAt   완료한 시각
 */
public record ScenarioCompletedResDto(
        Long scenarioId,
        String title,
        LocalDateTime completedAt
) {
    /**
     * ScenarioCompleted 엔티티에서 응답 DTO로 변환하는 정적 팩토리 메서드
     * @param entity    완료된 시나리오 엔티티
     * @return ScenarioCompletedResDto 응답 객체
     */
    public static ScenarioCompletedResDto from(ScenarioCompleted entity) {
        return new ScenarioCompletedResDto(
                entity.getScenario().getId(),
                entity.getScenario().getTitle(),
                entity.getCompletedAt()
        );
    }
}