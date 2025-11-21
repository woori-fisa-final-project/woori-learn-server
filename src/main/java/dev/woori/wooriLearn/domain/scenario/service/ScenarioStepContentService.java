package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.content.ChoiceContent;
import dev.woori.wooriLearn.domain.scenario.content.ChoiceOption;
import dev.woori.wooriLearn.domain.scenario.content.StepMeta;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.ChoiceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 시나리오 스텝의 content(JSON 문자열)를 전담해서 파싱하고 각종 DTO/도메인 모델로 매핑해 주는 서비스
 */
@Service
@RequiredArgsConstructor
public class ScenarioStepContentService {

    private final ObjectMapper objectMapper;

    /** ScenarioStep -> ProgressResumeResDto 매핑 (콘텐츠 JsonNode 포함) */
    public ProgressResumeResDto mapStep(ScenarioStep step) {
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
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝 content JSON 파싱 실패. stepId=" + step.getId()
            );
        }
    }

    /** Quiz 엔티티 -> QuizResDto 매핑 */
    public QuizResDto mapQuiz(Quiz quiz) {
        try {
            var listType = TypeFactory.defaultInstance()
                    .constructCollectionType(List.class, String.class);
            List<String> opts = objectMapper.readValue(quiz.getOptions(), listType);
            return new QuizResDto(quiz.getId(), quiz.getQuestion(), opts);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "퀴즈 options 파싱 실패. quizId=" + quiz.getId()
            );
        }
    }

    /** CHOICE 스텝 content -> ChoiceInfo 파싱 */
    public ChoiceInfo parseChoice(ScenarioStep step, int answerIndex) {
        try {
            ChoiceContent content = objectMapper.readValue(
                    step.getContent(),
                    ChoiceContent.class
            );

            if (content.choices() == null || content.choices().isEmpty()) {
                throw new CommonException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "CHOICE 스텝에 choices 배열이 비어있습니다. stepId=" + step.getId()
                );
            }

            if (answerIndex < 0 || answerIndex >= content.choices().size()) {
                throw new CommonException(
                        ErrorCode.INVALID_REQUEST,
                        "선택 인덱스 범위 초과. index=" + answerIndex
                );
            }

            ChoiceOption selected = content.choices().get(answerIndex);
            boolean good = Boolean.TRUE.equals(selected.good());
            Long next = selected.next(); // null 허용

            return new ChoiceInfo(good, next);

        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "CHOICE content 파싱 실패. stepId=" + step.getId()
            );
        }
    }

    /** 각 StepType 에 맞는 DTO로 content 파싱 후 meta 추출 */
    public Optional<StepMeta> getMeta(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            JsonNode metaNode = root.get("meta");
            if (metaNode == null || metaNode.isNull()) {
                return Optional.empty();
            }
            StepMeta meta = objectMapper.treeToValue(metaNode, StepMeta.class);
            return Optional.ofNullable(meta);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝 content JSON 파싱 실패. stepId=" + step.getId()
            );
        }
    }

    /** CHOICE 타입이 아닌 그 외 타입들 content 내부에 choices 배열이 존재하는지 여부 */
    public boolean hasChoices(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            JsonNode choicesNode = root.get("choices");
            return choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0;
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝 content JSON 파싱 실패(choices 확인). stepId=" + step.getId()
            );
        }
    }
}
