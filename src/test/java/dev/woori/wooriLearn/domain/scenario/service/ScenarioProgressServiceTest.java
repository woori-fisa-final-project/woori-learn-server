package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgressList;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressListRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioProgressServiceTest {

    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private ScenarioStepRepository stepRepository;
    @Mock
    private ScenarioProgressListRepository progressRepository;
    @Mock
    private ScenarioCompletedRepository completedRepository;
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScenarioProgressService service;

    private Users user;
    private Scenario scenario;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ScenarioProgressService(scenarioRepository, stepRepository, progressRepository, completedRepository, objectMapper);
        user = Users.builder().id(10L).userId("user").build();
        scenario = new Scenario(1L, "title", null);
    }

    @Test
    @DisplayName("resume: 진행기록 없으면 시작 스텝으로")
    void resume_noProgress() {
        // given: 시작 스텝을 반환하도록
        ScenarioStep start = ScenarioStep.builder().id(101L).scenario(scenario).type(StepType.DIALOG).content("{\"x\":1}").build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());
        when(stepRepository.findStartStepOrFail(1L)).thenReturn(start);

        // when
        ProgressResumeResDto res = service.resume(user, 1L);

        // then
        assertEquals(101L, res.nowStepId());
        assertEquals(StepType.DIALOG, res.type());
        assertNotNull(res.content());
    }

    @Test
    @DisplayName("advance: 퀴즈 없는 스텝 -> 다음 스텝으로")
    void advance_noQuizToNext() {
        // given: s1에는 퀴즈 없음
        ScenarioStep s2 = ScenarioStep.builder()
                .id(102L).scenario(scenario).type(StepType.DIALOG).content("{\"b\":2}")
                .build();
        ScenarioStep s1 = ScenarioStep.builder()
                .id(101L).scenario(scenario).type(StepType.DIALOG).content("{\"a\":1}").nextStep(s2)
                .build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findById(101L)).thenReturn(Optional.of(s1));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());

        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s1, s2));
        when(stepRepository.findStartStepOrFail(1L)).thenReturn(s1);

        // when
        AdvanceResDto res = service.advance(user, 1L, 101L, null);

        // then: 다음 스텝으로 이동되었고 퀴즈는 없음
        assertEquals(AdvanceStatus.ADVANCED, res.status());
        assertNotNull(res.step());
        assertEquals(102L, res.step().nowStepId());
        assertNull(res.quiz());
        verify(progressRepository).save(any(ScenarioProgressList.class));
    }

    @Test
    @DisplayName("advance: 퀴즈 스텝, 답 미제출 -> QUIZ_REQUIRED")
    void advance_quizRequired() {
        // given: 퀴즈가 있는 스텝(s1), 아직 답 미제출
        Quiz quiz = Quiz.builder()
                .question("Q?")
                .options("[\"A\",\"B\"]")
                .answer(1)
                .build();
        ScenarioStep s1 = ScenarioStep.builder().id(101L).scenario(scenario).type(StepType.CHOICE).content("{\"q\":true}").quiz(quiz).build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findById(101L)).thenReturn(Optional.of(s1));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());

        // when
        AdvanceResDto res = service.advance(user, 1L, 101L, null);

        // then: 퀴즈 정보가 담긴 QUIZ_REQUIRED 상태
        assertEquals(AdvanceStatus.QUIZ_REQUIRED, res.status());
        assertNotNull(res.quiz());
        assertEquals(quiz.getId(), res.quiz().id());
    }

    @Test
    @DisplayName("advance: 퀴즈 스텝, 오답 -> QUIZ_WRONG")
    void advance_quizWrong() {
        // given: 정답 인덱스 1, 제출은 0(오답)
        Quiz quiz = Quiz.builder()
                .question("Q?")
                .options("[\"A\",\"B\"]")
                .answer(1)
                .build();
        ScenarioStep s1 = ScenarioStep.builder().id(101L).scenario(scenario).type(StepType.CHOICE).content("{\"q\":true}").quiz(quiz).build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findById(101L)).thenReturn(Optional.of(s1));

        // when
        AdvanceResDto res = service.advance(user, 1L, 101L, 0);

        // then: 오답 상태 및 퀴즈 재노출
        assertEquals(AdvanceStatus.QUIZ_WRONG, res.status());
        assertNotNull(res.quiz());
    }

    @Test
    @DisplayName("advance: 마지막 스텝 -> COMPLETED, 진행기록 삭제")
    void advance_completed() {
        // given: nextStep이 없는 마지막 스텝
        ScenarioStep last = ScenarioStep.builder().id(199L).scenario(scenario).type(StepType.DIALOG).content("{\"z\":9}").build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findById(199L)).thenReturn(Optional.of(last));

        when(completedRepository.existsByUserAndScenario(user, scenario)).thenReturn(false);

        // when
        AdvanceResDto res = service.advance(user, 1L, 199L, null);

        // then: 완료 상태이며, 진행 기록 삭제가 호출되어야 함
        assertEquals(AdvanceStatus.COMPLETED, res.status());
        verify(progressRepository).deleteByUserAndScenario(user, scenario);
    }

    @Test
    @DisplayName("resume: 시나리오 없음 -> ENTITY_NOT_FOUND")
    void resume_notFoundScenario() {
        // given
        when(scenarioRepository.findById(9L)).thenReturn(Optional.empty());

        // when
        CommonException ex = assertThrows(CommonException.class, () -> service.resume(user, 9L));

        // then
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }
}
