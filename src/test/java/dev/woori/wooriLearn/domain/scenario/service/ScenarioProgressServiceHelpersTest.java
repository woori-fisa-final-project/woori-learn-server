package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.request.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsDepositService;
import dev.woori.wooriLearn.domain.scenario.content.StepMeta;
import dev.woori.wooriLearn.domain.scenario.entity.*;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import dev.woori.wooriLearn.domain.scenario.service.processor.ContentInfo;
import dev.woori.wooriLearn.domain.scenario.service.processor.StepProcessorResolver;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScenarioProgressServiceHelpersTest {

    @InjectMocks
    private ScenarioProgressService service;

    @Mock private UserRepository userRepository;
    @Mock private ScenarioRepository scenarioRepository;
    @Mock private ScenarioStepRepository stepRepository;
    @Mock private ScenarioProgressRepository progressRepository;
    @Mock private ScenarioCompletedRepository completedRepository;
    @Mock private PointsDepositService pointsDepositService;
    @Mock private StepProcessorResolver stepProcessorResolver;
    @Mock private ScenarioStepContentService contentService;

    private Scenario scenario;
    private ScenarioStep step;
    private ScenarioProgress progress;
    private Users user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = Scenario.builder().id(1L).title("s").totalNormalSteps(4).build();
        step = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .type(dev.woori.wooriLearn.domain.scenario.model.StepType.DIALOG)
                .normalIndex(2)
                .content("{}")
                .build();
        user = Users.builder().id(7L).userId("u").build();
        progress = ScenarioProgress.builder()
                .id(5L)
                .scenario(scenario)
                .user(user)
                .step(step)
                .progressRate(25.0)
                .build();
    }

    @Test
    void computeProgressRateOnNormalPath_returnsNullWhenInvalid() {
        assertNull(service.computeProgressRateOnNormalPath(null, step));
        assertNull(service.computeProgressRateOnNormalPath(scenario, null));
        Scenario badScenario = Scenario.builder().id(1L).title("s").totalNormalSteps(0).build();
        ScenarioStep badIndex = ScenarioStep.builder()
                .id(step.getId())
                .scenario(scenario)
                .type(step.getType())
                .content(step.getContent())
                .normalIndex(0)
                .build();
        assertNull(service.computeProgressRateOnNormalPath(badScenario, step));
        assertNull(service.computeProgressRateOnNormalPath(scenario, badIndex));
    }

    @Test
    void computeProgressRateOnNormalPath_calculatesAndNormalizes() {
        Double pct = service.computeProgressRateOnNormalPath(scenario, step);
        assertEquals(50.0, pct);
    }

    @Test
    void monotonicRate_neverDecreases() {
        double higher = service.monotonicRate(progress, 30.0);
        assertEquals(30.0, higher);
        double lower = service.monotonicRate(progress, 10.0);
        assertEquals(25.0, lower);
    }

    @Test
    void preloadStepsAsMap_throwsWhenEmpty() {
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of());
        CommonException ex = assertThrows(CommonException.class, () -> service.preloadStepsAsMap(1L));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }

    @Test
    void updateProgressAndSave_forceFreezeKeepsExistingRate() {
        when(progressRepository.save(any())).thenReturn(progress);
        double rate = service.updateProgressAndSave(progress, step, scenario, true);
        assertEquals(25.0, rate);
        verify(progressRepository).save(progress);
    }

    @Test
    void updateProgressAndSave_normalPathComputesRate() {
        ScenarioStep s = ScenarioStep.builder()
                .id(step.getId())
                .scenario(scenario)
                .type(step.getType())
                .content(step.getContent())
                .normalIndex(4)
                .build();
        progress = ScenarioProgress.builder()
                .id(progress.getId())
                .scenario(scenario)
                .user(user)
                .step(step)
                .progressRate(20.0)
                .build();
        when(progressRepository.save(any())).thenReturn(progress);

        double rate = service.updateProgressAndSave(progress, s, scenario, false);

        assertEquals(100.0, rate);
        verify(progressRepository).save(progress);
    }

    @Test
    void ensureCompletedOnce_returnsTrueOnFirstInsert() {
        when(completedRepository.saveAndFlush(any())).thenReturn(ScenarioCompleted.builder().build());
        assertTrue(service.ensureCompletedOnce(user, scenario));
    }

    @Test
    void ensureCompletedOnce_returnsFalseOnDuplicate() {
        when(completedRepository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));
        assertFalse(service.ensureCompletedOnce(user, scenario));
    }

    @Test
    void ensureCompletedOnceInsertIgnore_usesRepositoryInsertIgnore() {
        when(completedRepository.insertIgnore(7L, 1L)).thenReturn(1);
        assertTrue(service.ensureCompletedOnceInsertIgnore(user, scenario));
        when(completedRepository.insertIgnore(7L, 1L)).thenReturn(0);
        assertFalse(service.ensureCompletedOnceInsertIgnore(user, scenario));
    }

    @Test
    void loadStepRuntime_conflictWhenQuizAndChoices() {
        ScenarioStep start = ScenarioStep.builder()
                .id(9L)
                .scenario(scenario)
                .type(step.getType())
                .content("{}")
                .build();
        ScenarioStep withQuizAndChoice = ScenarioStep.builder()
                .id(10L)
                .scenario(scenario)
                .type(step.getType())
                .content("{}")
                .quiz(Quiz.builder().id(100L).question("q").options("[1]").answer(1).build())
                .build();
        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(start, withQuizAndChoice));
        when(stepRepository.findStartStepOrFail(1L)).thenReturn(start);
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.of(progress));
        when(contentService.parseContentInfo(withQuizAndChoice))
                .thenReturn(new ContentInfo(Optional.of(new StepMeta(null, null)), true));

        CommonException ex = assertThrows(CommonException.class,
                () -> service.advance(user, 1L, withQuizAndChoice.getId(), null));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void handleScenarioCompletion_rewardsOnceAndResetsProgress() {
        ScenarioStep start = ScenarioStep.builder()
                .id(9L)
                .scenario(scenario)
                .type(step.getType())
                .content(step.getContent())
                .normalIndex(1)
                .build();
        ScenarioProgress prg = ScenarioProgress.builder()
                .id(progress.getId())
                .scenario(scenario)
                .user(user)
                .step(start)
                .progressRate(90.0)
                .build();

        StepContext ctx = new StepContext(
                user,
                scenario,
                start,
                null,
                Map.of(start.getId(), start),
                prg,
                false,
                false,
                start.getId(),
                false
        );
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(completedRepository.insertIgnore(user.getId(), scenario.getId())).thenReturn(1);
        when(scenarioRepository.count()).thenReturn(1L);
        when(completedRepository.countByUser(user)).thenReturn(1L);

        service.handleScenarioCompletion(ctx);

        ArgumentCaptor<PointsDepositRequestDto> captor = ArgumentCaptor.forClass(PointsDepositRequestDto.class);
        verify(pointsDepositService, times(2)).depositPoints(eq("u"), captor.capture());
        // 첫 번째 1000, 두 번째 10000
        assertEquals(10000, captor.getAllValues().get(1).amount());
    }
}
