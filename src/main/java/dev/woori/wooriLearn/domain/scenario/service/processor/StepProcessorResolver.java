package dev.woori.wooriLearn.domain.scenario.service.processor;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.scenario.service.StepContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 시나리오 스텝의 타입/상태에 따라 어떤 수현체가 처리할지 결정해주는 Resolver
 * - CHOICE 스텝 -> ChoiceStepProcessor
 * - 배드 브랜치/배드 엔딩 -> BadBranchStepProcessor
 * - 퀴즈가 연결된 스텝 -> QuizGateStepProcessor
 * - 그 외 일반 스텝 -> NormalStepProcessor
 */
@Component
@RequiredArgsConstructor
public class StepProcessorResolver {

    private final ChoiceStepProcessor choiceStepProcessor;
    private final BadBranchStepProcessor badBranchStepProcessor;
    private final QuizGateStepProcessor quizGateStepProcessor;
    private final NormalStepProcessor normalStepProcessor;

    /**
     * 현재 스텝과 컨텍스트 정보를 기반으로 어떤 StepProcessor가 로직을 처리할지 선택
     * @param ctx 현재 사용자/스텝/메타 정보가 담긴 컨텍스트
     * @return 선택된 StepProcessor 구현체
     */
    public StepProcessor resolve(StepContext ctx) {
        ScenarioStep step = ctx.current();

        // 1) 배드 브랜치 / 배드 엔딩
        if (ctx.badBranch() || ctx.badEnding()) {
            return badBranchStepProcessor;
        }

        // 2) CHOICE 스텝
        if (step.getType() == StepType.CHOICE) {
            return choiceStepProcessor;
        }

        // 3) 퀴즈 게이트
        if (step.getQuiz() != null) {
            return quizGateStepProcessor;
        }

        // 4) 기본은 정상 next 처리
        return normalStepProcessor;
    }
}
