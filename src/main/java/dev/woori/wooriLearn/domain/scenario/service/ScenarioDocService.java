package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioDocDto;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioDocStepDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 시나리오 전체를 조회하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioDocService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ObjectMapper objectMapper;

    public ScenarioDocDto getScenarioDoc(Long scenarioId) {
        // 1) 시나리오 존재 확인
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오 없음: " + scenarioId));

        // 2) 모든 스텝 조회
        List<ScenarioStep> steps = stepRepository.findByScenarioIdWithNextStep(scenarioId);
        if (steps.isEmpty()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝이 비어있습니다. scenarioId=" + scenarioId);
        }

        // 3) 시작 스텝 계산 (중복 제거: repository default 메서드 사용)
        Long startId = stepRepository.findStartStepOrFail(scenarioId).getId();

        // 4) 문서 스텝 DTO 변환
        List<ScenarioDocStepDto> docSteps = steps.stream().map(s -> {
            try {
                JsonNode content = objectMapper.readTree(s.getContent());
                return new ScenarioDocStepDto(
                        s.getId(),
                        s.getType(),
                        s.getNextStep() != null ? s.getNextStep().getId() : null,
                        s.getQuiz() != null ? s.getQuiz().getId() : null,
                        content
                );
            } catch (JsonProcessingException e) {
                throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "content JSON 파싱 실패. stepId=" + s.getId());
            }
        }).toList();

        // 5) 최종 문서 반환
        return new ScenarioDocDto(scenario.getId(), scenario.getTitle(), startId, docSteps);
    }
}

