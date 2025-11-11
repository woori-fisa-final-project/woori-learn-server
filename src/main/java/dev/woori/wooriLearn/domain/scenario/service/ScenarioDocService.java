package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioDocDto;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioDocStepDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시나리오 문서 조회 서비스
 * 하나의 시나리오에 포함된 모든 스텝과 시작 스텝을 계산해 클라이언트가 초기 렌더링에 사용할 수 있는 문서 형태로 반환
 *
 * 주요기능
 *  - 시나리오 존재 여부 검증
 *  - 스텝 전체 조회 및 시작 스텝 결정
 *  - 각 스텝의 content(JSON 문자열)를 파싱
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioDocService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ObjectMapper objectMapper;

    /**
     * 시나리오 문서를 조회
     *
     * @param scenarioId 조회할 시나리오 ID
     * @return 시나리오 문서 DTO(제목, 시작 스텝 ID, 모든 스텝 목록)
     */
    public ScenarioDocDto getScenarioDoc(Long scenarioId) {
        // 1) 시나리오 존재 확인
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음: " + scenarioId));

        // 2) 해당 시나리오의 모든 스텝 조회
        var steps = stepRepository.findByScenarioId(scenarioId);
        if (steps.isEmpty()) throw new IllegalStateException("스텝이 비어있습니다. scenarioId=" + scenarioId);

        // 3) 시작 스텝 계산
        //  - 규칙: 같은 시나리오 내에서 다른 스텝의 nextStep으로 참조되지 않는 스텝 = 루트
        ScenarioStep startStep = stepRepository.findStartStep(scenarioId)
                .orElseGet(() -> stepRepository.findFirstByScenarioIdOrderByIdAsc(scenarioId)
                        .orElseThrow(() -> new IllegalStateException("시작 스텝을 찾지 못했습니다.")));

        Long startId = startStep.getId();

        // 4) 각 스텝을 문서 DTO로 변환
        //  - content는 JSON 문자열 -> JsonNode로 파싱
        //  - next/quiz는 존재 시 ID만 노출
        var docSteps = steps.stream().map(s -> {
            try {
                JsonNode content = objectMapper.readTree(s.getContent());
                return new ScenarioDocStepDto(
                        s.getId(),
                        s.getType(),
                        s.getNextStep() != null ? s.getNextStep().getId() : null,
                        s.getQuiz() != null ? s.getQuiz().getId() : null,
                        content
                );
            } catch (Exception e) {
                throw new IllegalStateException("content JSON 파싱 실패. stepId=" + s.getId(), e);
            }
        }).toList();

        // 5) 최종 문서 DTO 반환
        return new ScenarioDocDto(scenario.getId(), scenario.getTitle(), startId, docSteps);
    }
}
