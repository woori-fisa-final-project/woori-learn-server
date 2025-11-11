package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
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

import java.util.List;
import java.util.Objects;

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

        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgressList::getStep)
                .orElseGet(() ->
                        stepRepository.findStartStep(scenarioId)
                                .orElseGet(() -> stepRepository.findFirstByScenarioIdOrderByIdAsc(scenarioId)
                                        .orElseThrow(() -> new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 찾을 수 없습니다.")))
                );

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

        progress.moveToStep(next);
        progressRepository.save(progress);

        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    @Transactional
    public void saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
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

        progress.moveToStep(step);
        progressRepository.save(progress);
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

