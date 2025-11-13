package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressSaveResDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioProgressServiceTest {

    @Mock private ScenarioRepository scenarioRepository;
    @Mock private ScenarioStepRepository stepRepository;
    @Mock private ScenarioProgressListRepository progressRepository;
    @Mock private ScenarioCompletedRepository completedRepository;

    private ObjectMapper objectMapper;

    @InjectMocks
    private ScenarioProgressService service;

    private Users user;
    private Scenario scenario;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ScenarioProgressService(
                scenarioRepository, stepRepository, progressRepository, completedRepository, objectMapper
        );
        user = Users.builder().id(10L).userId("user").build();
        scenario = new Scenario(1L, "title", null);
    }

    @Test
    @DisplayName("resume: 진행기록 없으면 inferStartStepId로 계산한 시작 스텝 반환")
    void resume_noProgress_infersStart() {
        // main chain: 101(start) -> 102
        ScenarioStep s102 = step(102L, StepType.DIALOG, "{\"b\":2}");
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"a\":1}");
        linkNext(s101, s102);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s101, s102));

        ProgressResumeResDto res = service.resume(user, 1L);

        assertEquals(101L, res.nowStepId());
        assertEquals(StepType.DIALOG, res.type());
        assertNotNull(res.content());
    }

    @Test
    @DisplayName("advance: 퀴즈 없는 일반 스텝 -> 다음 스텝 ADVANCED")
    void advance_noQuizToNext() {
        // main chain: 101 -> 102
        ScenarioStep s102 = step(102L, StepType.DIALOG, "{\"b\":2}");
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"a\":1}");
        linkNext(s101, s102);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s101, s102));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());

        AdvanceResDto res = service.advance(user, 1L, 101L, null);

        assertEquals(AdvanceStatus.ADVANCED, res.status());
        assertNotNull(res.step());
        assertEquals(102L, res.step().nowStepId());
        verify(progressRepository).save(any(ScenarioProgressList.class));
    }

    @Test
    @DisplayName("advance: DIALOG에 퀴즈가 있고 답 미제출 -> QUIZ_REQUIRED")
    void advance_quizRequired_onDialog() {
        Quiz quiz = Quiz.builder()
                .id(777L).question("Q?").options("[\"A\",\"B\"]").answer(1)
                .build();
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"q\":true}");
        setQuiz(s101, quiz);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        // byId 로딩 필요
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s101));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());

        AdvanceResDto res = service.advance(user, 1L, 101L, null);

        assertEquals(AdvanceStatus.QUIZ_REQUIRED, res.status());
        assertNotNull(res.quiz());
        assertEquals(777L, res.quiz().id());
        verify(progressRepository).save(any());
    }

    @Test
    @DisplayName("advance: DIALOG 퀴즈 오답 -> QUIZ_WRONG")
    void advance_quizWrong_onDialog() {
        Quiz quiz = Quiz.builder()
                .id(778L).question("Q?").options("[\"A\",\"B\"]").answer(1)
                .build();
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"q\":true}");
        setQuiz(s101, quiz);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s101));

        AdvanceResDto res = service.advance(user, 1L, 101L, 0);

        assertEquals(AdvanceStatus.QUIZ_WRONG, res.status());
        assertNotNull(res.quiz());
        assertEquals(778L, res.quiz().id());
        verify(progressRepository).save(any());
    }

    @Test
    @DisplayName("advance: 마지막 스텝에서 COMPLETED -> 진행률 100 저장 + 시작 스텝으로 이동")
    void advance_completed_saves100_andMovesStart() {
        // only one step => also start
        ScenarioStep lastAndStart = step(199L, StepType.DIALOG, "{\"z\":9}");

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(lastAndStart));
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());
        when(completedRepository.existsByUserAndScenario(user, scenario)).thenReturn(false);

        AdvanceResDto res = service.advance(user, 1L, 199L, null);

        assertEquals(AdvanceStatus.COMPLETED, res.status());
        // 저장은 됨, 삭제는 안 됨
        verify(progressRepository, atLeastOnce()).save(any(ScenarioProgressList.class));
        verify(progressRepository, never()).deleteByUserAndScenario(any(), any());
        verify(completedRepository).save(any());
    }

    // ---- 진행률/분기 추가 테스트 ----

    @Test
    @DisplayName("진행률: 정루트(CHOICE good 경유) 4스텝 → 25/50/75/100%")
    void progressRate_mainChain_viaSaveCheckpoint() {
        /*
         * main chain: 101(start) -> 102 -> 104(CHOICE good->105) -> 105(last)
         * bad branch: 201 -> 202(badEnding)
         */
        ScenarioStep s105 = step(105L, StepType.DIALOG, "{\"t\":\"end\"}");
        ScenarioStep s104 = step(104L, StepType.CHOICE, """
            {"title":"how","choices":[
              {"good":true,"next":105,"text":"정상"},
              {"good":false,"next":201,"text":"수상"}
            ]}
            """);
        ScenarioStep s102 = step(102L, StepType.DIALOG, "{\"b\":2}");
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"a\":1}");
        linkNext(s101, s102);
        linkNext(s102, s104);

        ScenarioStep s201 = step(201L, StepType.DIALOG, "{\"meta\":{\"branch\":\"bad\",\"badEnding\":false}}");
        ScenarioStep s202 = step(202L, StepType.DIALOG, "{\"meta\":{\"branch\":\"bad\",\"badEnding\":true}}");
        linkNext(s201, s202);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L))
                .thenReturn(List.of(s101, s102, s104, s105, s201, s202));

        // 101 → 25%
        when(progressRepository.findByUserAndScenario(user, scenario)).thenReturn(Optional.empty());
        ProgressSaveResDto r1 = service.saveCheckpoint(user, 1L, 101L);
        assertEquals(25.0, r1.progressRate(), 0.0001);

        // 102 → 50%
        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s101, 25.0)));
        ProgressSaveResDto r2 = service.saveCheckpoint(user, 1L, 102L);
        assertEquals(50.0, r2.progressRate(), 0.0001);

        // 104(CHOICE) → 75%
        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s102, 50.0)));
        ProgressSaveResDto r3 = service.saveCheckpoint(user, 1L, 104L);
        assertEquals(75.0, r3.progressRate(), 0.0001);

        // 105 → 100%
        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s104, 75.0)));
        ProgressSaveResDto r4 = service.saveCheckpoint(user, 1L, 105L);
        assertEquals(100.0, r4.progressRate(), 0.0001);
    }

    @Test
    @DisplayName("진행률: 배드 브랜치 스텝 저장 시 동결(증가하지 않음)")
    void progressRate_badBranchFrozen() {
        // main: 101 -> 102 -> 104(CHOICE)
        ScenarioStep s104 = step(104L, StepType.CHOICE, """
            {"title":"how","choices":[
              {"good":true,"next":105,"text":"정상"},
              {"good":false,"next":201,"text":"수상"}
            ]}
            """);
        ScenarioStep s102 = step(102L, StepType.DIALOG, "{\"b\":2}");
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"a\":1}");
        linkNext(s101, s102);
        linkNext(s102, s104);

        ScenarioStep s201 = step(201L, StepType.DIALOG, "{\"meta\":{\"branch\":\"bad\",\"badEnding\":false}}");
        ScenarioStep s202 = step(202L, StepType.DIALOG, "{\"meta\":{\"branch\":\"bad\",\"badEnding\":true}}");
        linkNext(s201, s202);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L))
                .thenReturn(List.of(s101, s102, s104, s201, s202));

        // 현재 75% 달성 상태라고 가정(앵커 104)
        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s104, 75.0)));
        ProgressSaveResDto rBad1 = service.saveCheckpoint(user, 1L, 201L);
        assertEquals(75.0, rBad1.progressRate(), 0.0001);

        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s201, 75.0)));
        ProgressSaveResDto rBad2 = service.saveCheckpoint(user, 1L, 202L);
        assertEquals(75.0, rBad2.progressRate(), 0.0001);
    }

    @Test
    @DisplayName("advance: CHOICE에서 잘못된 선택 → ADVANCED_FROZEN, 진행률 증가 없음")
    void advance_choiceWrong_freeze() {
        ScenarioStep s104 = step(104L, StepType.CHOICE, """
            {"title":"how","choices":[
              {"good":true,"next":105,"text":"정상"},
              {"good":false,"next":201,"text":"수상"}
            ]}
            """);
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"a\":1}");
        linkNext(s101, s104);

        ScenarioStep s201 = step(201L, StepType.DIALOG, "{\"meta\":{\"branch\":\"bad\",\"badEnding\":false}}");

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s101, s104, s201));

        // 기존 진행률 50%
        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s104, 50.0)));

        AdvanceResDto res = service.advance(user, 1L, 104L, 1); // 오답(인덱스 1)

        assertEquals(AdvanceStatus.ADVANCED_FROZEN, res.status());
        assertNotNull(res.step());
        assertEquals(201L, res.step().nowStepId());

        ArgumentCaptor<ScenarioProgressList> cap = ArgumentCaptor.forClass(ScenarioProgressList.class);
        verify(progressRepository, atLeastOnce()).save(cap.capture());
        assertEquals(50.0, cap.getValue().getProgressRate(), 0.0001);
    }

    @Test
    @DisplayName("advance: CHOICE에서 정답 선택 → ADVANCED, 진행률 증가(최대 100)")
    void advance_choiceGood_increaseTo100() {
        ScenarioStep s105 = step(105L, StepType.DIALOG, "{\"t\":\"end\"}");
        ScenarioStep s104 = step(104L, StepType.CHOICE, """
            {"title":"how","choices":[
              {"good":true,"next":105,"text":"정상"},
              {"good":false,"next":201,"text":"수상"}
            ]}
            """);
        ScenarioStep s102 = step(102L, StepType.DIALOG, "{\"b\":2}");
        ScenarioStep s101 = step(101L, StepType.DIALOG, "{\"a\":1}");
        linkNext(s101, s102);
        linkNext(s102, s104);

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s101, s102, s104, s105));

        // 직전 진행률 50%
        when(progressRepository.findByUserAndScenario(user, scenario))
                .thenReturn(Optional.of(progress(user, scenario, s104, 50.0)));

        AdvanceResDto res = service.advance(user, 1L, 104L, 0); // good 선택

        assertEquals(AdvanceStatus.ADVANCED, res.status());
        assertNotNull(res.step());
        assertEquals(105L, res.step().nowStepId());

        ArgumentCaptor<ScenarioProgressList> cap = ArgumentCaptor.forClass(ScenarioProgressList.class);
        verify(progressRepository, atLeastOnce()).save(cap.capture());
        assertEquals(100.0, cap.getValue().getProgressRate(), 0.0001);
    }

    @Test
    @DisplayName("resume: 시나리오 없음 → ENTITY_NOT_FOUND")
    void resume_notFoundScenario() {
        when(scenarioRepository.findById(9L)).thenReturn(Optional.empty());
        CommonException ex = assertThrows(CommonException.class, () -> service.resume(user, 9L));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    // ---------- helpers ----------
    private ScenarioStep step(Long id, StepType type, String content) {
        return ScenarioStep.builder()
                .id(id).scenario(scenario).type(type).content(content).build();
    }

    private void linkNext(ScenarioStep from, ScenarioStep to) {
        try {
            Field f = ScenarioStep.class.getDeclaredField("nextStep");
            f.setAccessible(true);
            f.set(from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ScenarioProgressList progress(Users u, Scenario sc, ScenarioStep step, double rate) {
        return ScenarioProgressList.builder()
                .user(u).scenario(sc).step(step).progressRate(rate)
                .build();
    }

    private void setQuiz(ScenarioStep step, Quiz quiz) {
        try {
            Field f = ScenarioStep.class.getDeclaredField("quiz");
            f.setAccessible(true);
            f.set(step, quiz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
