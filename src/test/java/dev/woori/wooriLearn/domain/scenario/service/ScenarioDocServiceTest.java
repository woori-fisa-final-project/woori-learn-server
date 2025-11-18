package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioDocDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
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
class ScenarioDocServiceTest {

    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private ScenarioStepRepository stepRepository;
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScenarioDocService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ScenarioDocService(scenarioRepository, stepRepository, objectMapper);
    }

    @Test
    @DisplayName("시나리오가 없으면 ENTITY_NOT_FOUND")
    void getScenarioDoc_notFound() {
        // given
        when(scenarioRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        CommonException ex = assertThrows(CommonException.class, () -> service.getScenarioDoc(1L));

        // then
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("스텝이 비어있으면 INTERNAL_SERVER_ERROR")
    void getScenarioDoc_emptySteps() {
        // given
        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(
                Scenario.builder().id(1L).title("title").build()
        ));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of());

        // when
        CommonException ex = assertThrows(CommonException.class, () -> service.getScenarioDoc(1L));

        // then
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("정상 변환 - 시작 스텝 포함 전체 문서 반환")
    void getScenarioDoc_success() {
        // given
        Scenario scenario = Scenario.builder()
                .id(1L)
                .title("title")
                .build();

        // 스텝 s1 -> s2 구조
        ScenarioStep s2 = ScenarioStep.builder()
                .id(102L)
                .scenario(scenario)
                .type(StepType.DIALOG)
                .content("{\"text\":\"world\"}")
                .build();

        ScenarioStep s1 = ScenarioStep.builder()
                .id(101L)
                .scenario(scenario)
                .type(StepType.DIALOG)
                .content("{\"text\":\"hello\"}")
                .nextStep(s2) // s1 다음은 s2
                .build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s1, s2));
        // 시작 스텝 계산 메서드가 s1을 반환하도록
        when(stepRepository.findStartStepOrFail(1L)).thenReturn(s1);

        // when
        ScenarioDocDto dto = service.getScenarioDoc(1L);

        // then: 시나리오/제목/시작스텝/스텝 수 검증
        assertEquals(1L, dto.scenarioId());
        assertEquals("title", dto.title());
        assertEquals(101L, dto.startStepId());
        assertEquals(2, dto.steps().size());

        // then: 첫 스텝의 next 및 content(JSON 파싱 결과) 검증
        assertEquals(102L, dto.steps().get(0).next());
        assertEquals("hello", dto.steps().get(0).content().get("text").asText());
    }

    @Test
    @DisplayName("content JSON 파싱 실패시 INTERNAL_SERVER_ERROR")
    void getScenarioDoc_parseFail() {
        // given
        Scenario scenario = Scenario.builder()
                .id(1L)
                .title("title")
                .build();

        // 잘못된 JSON 문자열을 가진 스텝
        ScenarioStep s1 = ScenarioStep.builder()
                .id(101L)
                .scenario(scenario)
                .type(StepType.DIALOG)
                .content("{invalid-json}")
                .build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(stepRepository.findByScenarioIdWithNextStep(1L)).thenReturn(List.of(s1));
        when(stepRepository.findStartStepOrFail(1L)).thenReturn(s1);

        // when
        CommonException ex = assertThrows(CommonException.class, () -> service.getScenarioDoc(1L));

        // then
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }
}

