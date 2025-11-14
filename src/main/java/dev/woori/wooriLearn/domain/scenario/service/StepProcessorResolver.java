package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepProcessorResolver {

    private final ChoiceStepProcessor choiceStepProcessor;
    private final BadBranchStepProcessor badBranchStepProcessor;
    private final QuizGateStepProcessor quizGateStepProcessor;
    private final NormalStepProcessor normalStepProcessor;

    public StepProcessor resolve(StepContext ctx) {
        ScenarioStep step = ctx.current();

        // 1) CHOICE 스텝
        if (step.getType() == StepType.CHOICE) {
            return choiceStepProcessor;
        }

        // 2) 배드 브랜치 / 배드 엔딩
        if (ctx.badBranch() || ctx.badEnding()) {
            return badBranchStepProcessor;
        }

        // 3) 퀴즈 게이트
        if (step.getQuiz() != null) {
            return quizGateStepProcessor;
        }

        // 4) 기본은 정상 next 처리
        return normalStepProcessor;
    }
}
