package dev.woori.wooriLearn.domain.scenario.service.processor;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BadBranchStepProcessorTest {

    @InjectMocks
    private BadBranchStepProcessor processor;
    @Mock
    private ScenarioProgressService service;

    private Scenario scenario;
    private ScenarioStep current;
    private ScenarioProgress progress;
    private ScenarioStep start;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = Scenario.builder().id(1L).title("s").totalNormalSteps(2).build();
        start = ScenarioStep.builder().id(5L).scenario(scenario).content("{}").build();
        current = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .content("{}")
                .build();
        progress = ScenarioProgress.builder()
                .id(2L)
                .scenario(scenario)
                .step(current)
                .progressRate(0.0)
                .build();
    }

    private StepContext ctx(boolean badEnding, ScenarioStep next) {
        Map<Long, ScenarioStep> byId = (next == null)
                ? Map.of(current.getId(), current, start.getId(), start)
                : Map.of(current.getId(), current, start.getId(), start, next.getId(), next);
        return new StepContext(null, scenario, current, null, byId, progress, true, badEnding, start.getId(), false);
    }

    @Test
    void badEndingUsesAnchorAndFreezes() {
        ScenarioStep anchor = ScenarioStep.builder().id(20L).scenario(scenario).content("{}").build();
        current = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .content("{}")
                .nextStep(anchor)
                .build();
        StepContext context = ctx(true, anchor);

        AdvanceResDto res = processor.process(context, service);
        verify(service).updateProgressAndSave(progress, anchor, scenario, true);
        assertEquals(AdvanceStatus.BAD_ENDING, res.status());
    }

    @Test
    void badEndingWithoutAnchor_usesStartOrThrows() {
        StepContext context = ctx(true, null);
        AdvanceResDto res = processor.process(context, service);
        verify(service).updateProgressAndSave(progress, start, scenario, true);
        assertEquals(AdvanceStatus.BAD_ENDING, res.status());
    }

    @Test
    void badBranchWithoutNext_returnsBadEnding() {
        StepContext context = ctx(false, null);
        AdvanceResDto res = processor.process(context, service);
        verify(service).updateProgressAndSave(progress, start, scenario, true);
        assertEquals(AdvanceStatus.BAD_ENDING, res.status());
    }

    @Test
    void badBranchWithNext_advancesFrozen() {
        ScenarioStep next = ScenarioStep.builder().id(30L).scenario(scenario).content("{}").build();
        current = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .content("{}")
                .nextStep(next)
                .build();
        StepContext context = ctx(false, next);
        AdvanceResDto res = processor.process(context, service);
        verify(service).updateProgressAndSave(progress, next, scenario, true);
        assertEquals(AdvanceStatus.ADVANCED_FROZEN, res.status());
    }

    @Test
    void missingAnchorThrows() {
        StepContext context = new StepContext(null, scenario, current, null, Map.of(current.getId(), current), progress, true, true, 0L, false);
        CommonException ex = assertThrows(CommonException.class, () -> processor.process(context, service));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }
}
