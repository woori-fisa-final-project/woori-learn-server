package dev.woori.wooriLearn.domain.scenario.service.processor;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.model.ChoiceInfo;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioProgressService;
import dev.woori.wooriLearn.domain.scenario.service.StepContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChoiceStepProcessorTest {

    @InjectMocks
    private ChoiceStepProcessor processor;

    @Mock
    private ScenarioProgressService service;

    private Scenario scenario;
    private ScenarioStep current;
    private ScenarioProgress progress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = Scenario.builder().id(1L).title("s").totalNormalSteps(2).build();
        current = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .content("{}")
                .build();
        progress = ScenarioProgress.builder()
                .id(5L)
                .scenario(scenario)
                .step(current)
                .progressRate(0.0)
                .build();
    }

    private StepContext ctx(Integer answer, Map<Long, ScenarioStep> byId, boolean badBranch, boolean badEnding) {
        return new StepContext(null, scenario, current, answer, byId, progress, badBranch, badEnding, current.getId(), true);
    }

    @Test
    @DisplayName("답을 선택하지 않으면 CHOICE_REQUIRED로 응답하고 진행률을 동결한다")
    void answerNull_returnsChoiceRequiredAndFreezes() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        AdvanceResDto res = processor.process(ctx(null, byId, false, false), service);

        verify(service).updateProgressAndSave(progress, current, scenario, true);
        assertEquals(AdvanceStatus.CHOICE_REQUIRED, res.status());
    }

    @Test
    @DisplayName("오답이고 다음 스텝이 없으면 BAD_ENDING을 반환한다")
    void badChoiceWithoutNext_returnsBadEnding() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        when(service.parseChoice(current, 0)).thenReturn(new ChoiceInfo(false, null));

        AdvanceResDto res = processor.process(ctx(0, byId, false, false), service);
        verify(service).updateProgressAndSave(progress, current, scenario, true);
        assertEquals(AdvanceStatus.BAD_ENDING, res.status());
    }

    @Test
    @DisplayName("오답이지만 다음 스텝이 있으면 ADVANCED_FROZEN을 반환한다")
    void badChoiceWithNext_advancesFrozen() {
        ScenarioStep next = ScenarioStep.builder().id(20L).scenario(scenario).content("{}").build();
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current, next.getId(), next);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(false, next.getId()));

        AdvanceResDto res = processor.process(ctx(1, byId, false, false), service);
        verify(service).updateProgressAndSave(progress, next, scenario, true);
        assertEquals(AdvanceStatus.ADVANCED_FROZEN, res.status());
    }

    @Test
    @DisplayName("다음 스텝 정보를 찾지 못하면 ENTITY_NOT_FOUND 예외를 던진다")
    void badChoiceNextMissing_throwsNotFound() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(false, 999L));
        CommonException ex = assertThrows(CommonException.class, () -> processor.process(ctx(1, byId, false, false), service));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("정답이고 다음 스텝이 없으면 시나리오를 완료한다")
    void goodChoiceNoNext_completesScenario() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(true, null));
        AdvanceResDto expected = new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        when(service.handleScenarioCompletion(any())).thenReturn(expected);

        AdvanceResDto res = processor.process(ctx(1, byId, false, false), service);
        assertEquals(expected, res);
    }

    @Test
    @DisplayName("정답이고 다음 스텝이 있으면 정상 진행하며 배드 브랜치에서는 동결한다")
    void goodChoiceNext_advancesNormalOrFrozen() {
        ScenarioStep next = ScenarioStep.builder().id(20L).scenario(scenario).content("{}").build();
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current, next.getId(), next);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(true, next.getId()));
        when(service.mapStep(next)).thenReturn(null);

        AdvanceResDto res = processor.process(ctx(1, byId, false, false), service);
        verify(service).updateProgressAndSave(progress, next, scenario, false);
        assertEquals(AdvanceStatus.ADVANCED, res.status());

        // frozen when in bad branch
        res = processor.process(ctx(1, byId, true, false), service);
        ArgumentCaptor<AdvanceStatus> statusCaptor = ArgumentCaptor.forClass(AdvanceStatus.class);
        verify(service, atLeastOnce()).updateProgressAndSave(progress, next, scenario, true);
        assertEquals(AdvanceStatus.ADVANCED_FROZEN, res.status());
    }
}
