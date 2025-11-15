package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;

/**
 * 시나리오의 각 스텝 타입에 대한 처리 전략을 정의하는 인터페이스
 * - NormalStepProcessor:       일반 스텝의 단순 next 처리
 * - ChoiceStepProcessor:       CHOICE 스텝 분기 처리
 * - QuizGateStepProcessor:     퀴즈 정답 여부에 따른 진행 제어
 * - BadBranchStepProcessor:    배드 브랜치/배드 엔딩 처리
 */
public interface StepProcessor {
    /**
     * 현재 스텝에 대한 처리 로직을 수행하고 다음 상태/스텝 정보를 담은 응담을 반환한다.
     * @param ctx       현재 사용자, 시나리오, 스텝, 입력값 등이 담긴 컨텍스트
     * @param service   진행률 계산, 완료 처리 등 공통 로직을 제공하는 서비스
     * @return 다음 스텝 정보와 상태 코드를 포함한 응답 DTO
     */
    AdvanceResDto process(StepContext ctx, ScenarioProgressService service);
}
