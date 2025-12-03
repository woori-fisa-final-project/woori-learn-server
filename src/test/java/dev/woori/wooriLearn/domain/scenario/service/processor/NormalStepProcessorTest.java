package dev.woori.wooriLearn.domain.scenario.service.processor;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NormalStepProcessorTest {

    @InjectMocks
    private NormalStepProcessor processor;
    @Mock
    private ScenarioProgressService service;

    private Scenario scenario;
    private ScenarioStep current;
    private ScenarioProgress progress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = Scenario.builder().id(1L).title("s").totalNormalSteps(2).build();
        current = ScenarioStep.builder().id(10L).scenario(scenario).content("{}").build();
        progress = ScenarioProgress.builder().id(3L).scenario(scenario).step(current).progressRate(0.0).build();
    }

    private StepContext ctx(ScenarioStep next) {
        ScenarioStep curr = (next == null) ? current : ScenarioStep.builder()
                .id(current.getId())
                .scenario(scenario)
                .content(current.getContent())
                .nextStep(next)
                .build();
        Map<Long, ScenarioStep> map = (next == null)
                ? Map.of(curr.getId(), curr)
                : Map.of(curr.getId(), curr, next.getId(), next);
        Long startId = curr.getId();
        return new StepContext(null, scenario, curr, null, map, progress, false, false, startId, false);
    }

    @Test
    void noNextStep_callsCompletion() {
        AdvanceResDto expected = new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        when(service.handleScenarioCompletion(any())).thenReturn(expected);

        AdvanceResDto res = processor.process(ctx(null), service);
        assertEquals(expected, res);
    }

    @Test
    void nextStep_advancesAndMaps() {
        ScenarioStep next = ScenarioStep.builder().id(20L).scenario(scenario).content("{}").build();
        when(service.mapStep(next)).thenReturn(null);

        AdvanceResDto res = processor.process(ctx(next), service);
        verify(service).updateProgressAndSave(progress, next, scenario, false);
        assertEquals(AdvanceStatus.ADVANCED, res.status());
    }
}
