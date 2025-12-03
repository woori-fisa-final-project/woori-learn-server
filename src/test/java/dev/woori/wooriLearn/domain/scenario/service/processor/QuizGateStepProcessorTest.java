package dev.woori.wooriLearn.domain.scenario.service.processor;

import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioProgressService;
import dev.woori.wooriLearn.domain.scenario.service.StepContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizGateStepProcessorTest {

    @InjectMocks
    private QuizGateStepProcessor processor;
    @Mock
    private NormalStepProcessor normalStepProcessor;
    @Mock
    private ScenarioProgressService service;

    private Scenario scenario;
    private ScenarioStep step;
    private ScenarioProgress progress;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = Scenario.builder().id(1L).title("s").totalNormalSteps(2).build();
        quiz = Quiz.builder().id(30L).question("q").options("[1]").answer(1).build();
        step = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .content("{}")
                .quiz(quiz)
                .build();
        progress = ScenarioProgress.builder()
                .id(5L)
                .scenario(scenario)
                .step(step)
                .progressRate(0.0)
                .build();
    }

    private StepContext ctx(Integer answer) {
        return new StepContext(null, scenario, step, answer, Map.of(step.getId(), step), progress, false, false, step.getId(), false);
    }

    @Test
    void noQuizDelegatesToNormal() {
        ScenarioStep noQuiz = ScenarioStep.builder()
                .id(step.getId())
                .scenario(scenario)
                .content("{}")
                .build();
        StepContext context = new StepContext(null, scenario, noQuiz, null, Map.of(noQuiz.getId(), noQuiz), progress, false, false, noQuiz.getId(), false);
        AdvanceResDto expected = new AdvanceResDto(AdvanceStatus.ADVANCED, null, null);
        when(normalStepProcessor.process(context, service)).thenReturn(expected);

        AdvanceResDto res = processor.process(context, service);
        assertEquals(expected, res);
    }

    @Test
    void quizAnswerNull_requiresAnswerAndFreezes() {
        QuizResDto quizDto = new QuizResDto(quiz.getId(), quiz.getQuestion(), java.util.List.of("1"));
        when(service.mapQuiz(quiz)).thenReturn(quizDto);
        when(service.mapStep(step)).thenReturn(null);

        AdvanceResDto res = processor.process(ctx(null), service);
        verify(service).updateProgressAndSave(progress, step, null, true);
        assertEquals(AdvanceStatus.QUIZ_REQUIRED, res.status());
        assertEquals(quizDto, res.quiz());
    }

    @Test
    void quizAnswerWrong_marksWrong() {
        QuizResDto quizDto = new QuizResDto(quiz.getId(), quiz.getQuestion(), java.util.List.of("1"));
        when(service.mapQuiz(quiz)).thenReturn(quizDto);
        when(service.mapStep(step)).thenReturn(null);

        AdvanceResDto res = processor.process(ctx(99), service);
        verify(service).updateProgressAndSave(progress, step, null, true);
        assertEquals(AdvanceStatus.QUIZ_WRONG, res.status());
    }

    @Test
    void quizAnswerCorrect_delegatesToNormal() {
        AdvanceResDto expected = new AdvanceResDto(AdvanceStatus.ADVANCED, null, null);
        when(normalStepProcessor.process(any(), eq(service))).thenReturn(expected);

        AdvanceResDto res = processor.process(ctx(1), service);
        assertEquals(expected, res);
    }
}
