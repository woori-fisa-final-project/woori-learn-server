package dev.woori.wooriLearn.domain.scenario.dto;

import java.util.List;

/**
 * 시나리오 문서 응답 DTO
 * 클라이언트가 시나리오 화면을 초기 구성할 때 필요한 전체 정보 제공
 *
 * @param scenarioId    시나리오 ID
 * @param title         시나리오 제목
 * @param startStepId   시작 스텝 ID (재개 이력이 없을 때 최초로 렌더링할 스텝)
 * @param steps         시나리오에 포함된 모든 스텝 목록
 */
public record ScenarioDocDto(
        Long scenarioId,
        String title,
        Long startStepId,
        List<ScenarioDocStepDto> steps
) {}
