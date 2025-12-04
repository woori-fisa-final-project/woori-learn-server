package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.user.entity.Users;

import java.util.Map;

/**
 * 시나리오 진행 스텝을 처리할 떄 필요한 컨텍스트 정보를 한 번에 묶어 전달하기 위한 레코드
 * @param user          현재 시나리오를 진행 중인 사용자 엔티티
 * @param scenario      진행 중인 시나리오 엔티티
 * @param current       지금 처리 중인 현재 스텝 엔티티
 * @param answer        사용자 입력(퀴즈/선택지)에 해당하는 답안
 * @param byId          해당 시나리오의 모든 스텝을 id 기준으로 모아둔 Map (N+1 방지를 위해 미리 로딩)
 * @param progress      사용자의 시나리오 진행 상태 엔티티
 * @param badBranch     현재 스텝이 배드 브랜치에 속하는지 여부
 * @param badEnding     현재 스텝이 배드 엔딩 스텝인지 여부
 * @param startStepId   시나리오의 시작 스텝 ID
 * @param hasChoices    현재 스텝의 content 안에 choices 배열이 존재하는지 여부
 */
public record StepContext(
        Users user,
        Scenario scenario,
        ScenarioStep current,
        Integer answer,
        Map<Long, ScenarioStep> byId,
        ScenarioProgress progress,
        boolean badBranch,
        boolean badEnding,
        Long startStepId,
        boolean hasChoices
) {
    public ScenarioStep startStep() {
        return byId.get(startStepId);
    }
}
