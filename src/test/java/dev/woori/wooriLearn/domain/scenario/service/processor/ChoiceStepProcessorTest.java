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
    void answerNull_returnsChoiceRequiredAndFreezes() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        AdvanceResDto res = processor.process(ctx(null, byId, false, false), service);

        verify(service).updateProgressAndSave(progress, current, scenario, true);
        assertEquals(AdvanceStatus.CHOICE_REQUIRED, res.status());
    }

    @Test
    void badChoiceWithoutNext_returnsBadEnding() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        when(service.parseChoice(current, 0)).thenReturn(new ChoiceInfo(false, null));

        AdvanceResDto res = processor.process(ctx(0, byId, false, false), service);
        verify(service).updateProgressAndSave(progress, current, scenario, true);
        assertEquals(AdvanceStatus.BAD_ENDING, res.status());
    }

    @Test
    void badChoiceWithNext_advancesFrozen() {
        ScenarioStep next = ScenarioStep.builder().id(20L).scenario(scenario).content("{}").build();
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current, next.getId(), next);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(false, next.getId()));

        AdvanceResDto res = processor.process(ctx(1, byId, false, false), service);
        verify(service).updateProgressAndSave(progress, next, scenario, true);
        assertEquals(AdvanceStatus.ADVANCED_FROZEN, res.status());
    }

    @Test
    void badChoiceNextMissing_throwsNotFound() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(false, 999L));
        CommonException ex = assertThrows(CommonException.class, () -> processor.process(ctx(1, byId, false, false), service));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void goodChoiceNoNext_completesScenario() {
        Map<Long, ScenarioStep> byId = Map.of(current.getId(), current);
        when(service.parseChoice(current, 1)).thenReturn(new ChoiceInfo(true, null));
        AdvanceResDto expected = new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        when(service.handleScenarioCompletion(any())).thenReturn(expected);

        AdvanceResDto res = processor.process(ctx(1, byId, false, false), service);
        assertEquals(expected, res);
    }

    @Test
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
