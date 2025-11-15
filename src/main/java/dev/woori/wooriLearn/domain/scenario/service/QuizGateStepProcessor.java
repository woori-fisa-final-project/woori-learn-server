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

/**
 * 퀴즈가 포함된 스텝을 처리하는 Processor
 *
 * - 현재 스텝에 QUIZ가 없으면 일반 스텝처럼 처리
 * - 퀴즈가 있고
 *      - 정답을 제출하지 않았거나, 제출한 답이 정답과 다르면
 *          -> QUIZ_REQUIRED 또는 QUIZ_WRONG 상태와 함께 현재 스텝 + 퀴즈 정보 반환
 *      - 제출한 답이 정답이면
 *          -> 정상 next 진행 처리
 */
@Component
@RequiredArgsConstructor
public class QuizGateStepProcessor implements StepProcessor {

    // 퀴즈를 통과한 이후에는 일반 스텝과 동일한 규칙으로 처리하기 위해 재사용
    private final NormalStepProcessor normalStepProcessor;

    @Override
    public AdvanceResDto process(StepContext ctx, ScenarioProgressService service) {
        ScenarioStep current = ctx.current();       // 현재 퀴즈 스텝
        ScenarioProgress progress = ctx.progress(); // 사용자 진행 상태
        Integer answer = ctx.answer();              // 사용자가 제출한 답

        Quiz quiz = current.getQuiz();
        if (quiz == null) {
            // 퀴즈가 없으면 그냥 정상 next 처리
            return normalStepProcessor.process(ctx, service);
        }

        // 정답 판정: answer가 null이 아니고, quiz.answer와 같은지 여부
        boolean isCorrect = (answer != null) && Objects.equals(quiz.getAnswer(), answer);
        if (!isCorrect) {
            // 미제출/오답인 경우
            // - 현재 스텝 위치 유지
            // - 진행률 동결
            service.updateProgressAndSave(progress, current, null, true);

            // 상태 코드 분기: 미제출/오답
            AdvanceStatus status = (answer == null)
                    ? AdvanceStatus.QUIZ_REQUIRED
                    : AdvanceStatus.QUIZ_WRONG;

            // 프론트에서 다시 퀴즈를 렌더링할 수 있도록 Quiz DTO 전달
            QuizResDto quizDto = service.mapQuiz(quiz);
            return new AdvanceResDto(status, service.mapStep(current), quizDto);
        }

        // 정답인 경우 정상 next 처리
        return normalStepProcessor.process(ctx, service);
    }
}
