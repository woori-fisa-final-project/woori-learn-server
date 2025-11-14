package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class QuizGateStepProcessor implements StepProcessor {

    private final NormalStepProcessor normalStepProcessor;

    @Override
    public AdvanceResDto process(StepContext ctx, ScenarioProgressService service) {
        ScenarioStep current = ctx.current();
        ScenarioProgress progress = ctx.progress();
        Integer answer = ctx.answer();

        Quiz quiz = current.getQuiz();
        if (quiz == null) {
            // 방어적: 퀴즈가 없으면 그냥 정상 next 처리
            return normalStepProcessor.process(ctx, service);
        }

        boolean isCorrect = (answer != null) && Objects.equals(quiz.getAnswer(), answer);
        if (!isCorrect) {
            // 미제출/오답 → 현재 스텝 유지(진행률 동결)
            service.updateProgressAndSave(progress, current, null, true);
            AdvanceStatus status = (answer == null)
                    ? AdvanceStatus.QUIZ_REQUIRED
                    : AdvanceStatus.QUIZ_WRONG;
            QuizResDto quizDto = service.mapQuiz(quiz);
            return new AdvanceResDto(status, service.mapStep(current), quizDto);
        }

        // 정답이면 정상 next 처리 로직으로 위임
        return normalStepProcessor.process(ctx, service);
    }
}
