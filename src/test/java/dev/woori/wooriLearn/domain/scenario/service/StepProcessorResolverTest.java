package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.scenario.service.processor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StepProcessorResolverTest {

    @InjectMocks
    private StepProcessorResolver resolver;

    @Mock private ChoiceStepProcessor choiceStepProcessor;
    @Mock private BadBranchStepProcessor badBranchStepProcessor;
    @Mock private QuizGateStepProcessor quizGateStepProcessor;
    @Mock private NormalStepProcessor normalStepProcessor;

    private Scenario scenario;
    private ScenarioStep step;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = Scenario.builder().id(1L).title("s").totalNormalSteps(1).build();
        step = ScenarioStep.builder().id(10L).scenario(scenario).type(StepType.DIALOG).content("{}").build();
    }

    private StepContext ctx(boolean hasChoices, boolean badBranch, boolean badEnding, boolean hasQuiz) {
        ScenarioStep curr = hasQuiz ? ScenarioStep.builder()
                .id(step.getId())
                .scenario(scenario)
                .type(step.getType())
                .content(step.getContent())
                .quiz(Quiz.builder().id(99L).question("q").options("[1]").answer(1).build())
                .build()
                : step;
        return new StepContext(null, scenario, curr, null, Map.of(curr.getId(), curr), null, badBranch, badEnding, curr.getId(), hasChoices);
    }

    @Test
    @DisplayName("선택지가 있으면 ChoiceStepProcessor를 우선 선택한다")
    void resolve_choiceHasPriority() {
        StepProcessor result = resolver.resolve(ctx(true, false, false, false));
        assertEquals(choiceStepProcessor, result);
    }

    @Test
    @DisplayName("배드 브랜치 플래그가 있으면 BadBranchStepProcessor를 반환한다")
    void resolve_badBranchSecond() {
        StepProcessor result = resolver.resolve(ctx(false, true, false, false));
        assertEquals(badBranchStepProcessor, result);
    }

    @Test
    @DisplayName("퀴즈가 있으면 QuizGateStepProcessor를 반환한다")
    void resolve_quizThird() {
        StepProcessor result = resolver.resolve(ctx(false, false, false, true));
        assertEquals(quizGateStepProcessor, result);
    }

    @Test
    @DisplayName("기본적으로 NormalStepProcessor를 반환한다")
    void resolve_defaultNormal() {
        StepProcessor result = resolver.resolve(ctx(false, false, false, false));
        assertEquals(normalStepProcessor, result);
    }
}
