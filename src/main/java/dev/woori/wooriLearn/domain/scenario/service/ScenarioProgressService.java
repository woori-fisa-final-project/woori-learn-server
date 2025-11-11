package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressSaveResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgressList;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressListRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class ScenarioProgressService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ScenarioProgressListRepository progressRepository;
    private final ScenarioCompletedRepository completedRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        // 진행 이력 -> 없으면 시작 스텝(중복 제거: repository default 메서드 사용)
        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgressList::getStep)
                .orElseGet(() -> stepRepository.findStartStepOrFail(scenarioId));

        return mapStep(step);
    }

    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        ScenarioStep current = stepRepository.findById(nowStepId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않습니다. id=" + nowStepId));
        if (!Objects.equals(current.getScenario().getId(), scenario.getId())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "스텝이 해당 시나리오에 속하지 않습니다.");
        }

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElse(ScenarioProgressList.builder()
                        .user(user)
                        .scenario(scenario)
                        .step(current)
                        .progressRate(0.0)
                        .build());

        Quiz quiz = current.getQuiz();
        if (quiz != null) {
            boolean isCorrect = answer != null && Objects.equals(quiz.getAnswer(), answer);
            if (!isCorrect) {
                progress.moveToStep(current);
                progressRepository.save(progress);
                AdvanceStatus status = (answer == null) ? AdvanceStatus.QUIZ_REQUIRED : AdvanceStatus.QUIZ_WRONG;
                return new AdvanceResDto(status, mapStep(current), mapQuiz(quiz));
            }
        }

        ScenarioStep next = current.getNextStep();
        if (next == null) {
            completedRepository.findByUserAndScenario(user, scenario)
                    .orElseGet(() -> completedRepository.save(
                            ScenarioCompleted.builder().user(user).scenario(scenario).build()
                    ));

            progressRepository.deleteByUserAndScenario(user, scenario);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        double rate = computeProgressRate(scenarioId, next.getId());
        progress.moveToStep(next, rate);
        progressRepository.save(progress);

        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    @Transactional
    public ProgressSaveResDto saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));
        ScenarioStep step = stepRepository.findById(nowStepId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않습니다. id=" + nowStepId));

        if (!Objects.equals(step.getScenario().getId(), scenario.getId())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "스텝이 시나리오와 일치하지 않습니다. stepId=" +
                    step.getId() + ", scenarioId=" + scenario.getId());
        }

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user)
                        .scenario(scenario)
                        .progressRate(0.0)
                        .build());

        // 진행률 계산
        double rate = computeProgressRate(scenarioId, nowStepId);

        // 스텝 이동 + 진행률 갱신
        progress.moveToStep(step, rate);
        progressRepository.save(progress);

        return new ProgressSaveResDto(scenarioId, nowStepId, rate);
    }

    private double computeProgressRate(Long scenarioId, Long nowStepId) {
        var steps = stepRepository.findByScenarioId(scenarioId);
        if (steps.isEmpty()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝이 비어있습니다. scenarioId=" + scenarioId);
        }

        // map 구성
        Map<Long, ScenarioStep> byId = steps.stream()
                .collect(toMap(ScenarioStep::getId, java.util.function.Function.identity()));

        // start부터 nextStep 체인을 따라가며 순서 계산(루프 방지)
        ScenarioStep cur = stepRepository.findStartStepOrFail(scenarioId);
        Set<Long> visited = new HashSet<>();

        int total = 0;
        Integer foundIdx = null;

        while (cur != null && !visited.contains(cur.getId())) {
            visited.add(cur.getId());
            if (cur.getId().equals(nowStepId)) {
                foundIdx = total; // 0-based index
            }
            total++;
            cur = (cur.getNextStep() != null) ? byId.get(cur.getNextStep().getId()) : null;
        }

        // 체인에 포함되지 않은 경우(id 오름차순 폴백)
        if (foundIdx == null) {
            var ordered = steps.stream().map(ScenarioStep::getId).sorted().toList();
            int pos = ordered.indexOf(nowStepId);
            if (pos < 0) pos = 0;
            total = ordered.size();
            foundIdx = pos;
        }

        double pct = ((foundIdx + 1) * 100.0) / Math.max(total, 1);
        // 소수 한 자리 반올림 등 필요 시 적용 가능
        return Math.min(100.0, Math.max(0.0, pct));
    }

    private ProgressResumeResDto mapStep(ScenarioStep step) {
        try {
            JsonNode contentNode = objectMapper.readTree(step.getContent());
            return new ProgressResumeResDto(
                    step.getScenario().getId(),
                    step.getId(),
                    step.getType(),
                    step.getQuiz() != null ? step.getQuiz().getId() : null,
                    contentNode
            );
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝 content JSON 파싱 실패. stepId=" + step.getId());
        }
    }

    private QuizResDto mapQuiz(Quiz quiz) {
        try {
            var listType = TypeFactory.defaultInstance()
                    .constructCollectionType(List.class, String.class);
            List<String> opts = objectMapper.readValue(quiz.getOptions(), listType);
            return new QuizResDto(quiz.getId(), quiz.getQuestion(), opts);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 options 파싱 실패. quizId=" + quiz.getId());
        }
    }
}

