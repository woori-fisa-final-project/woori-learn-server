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
import dev.woori.wooriLearn.domain.scenario.service.processor.ContentInfo;
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

    /**
     * 스텝 content(JSON)를 파싱하여 메타 정보와 choices 존재 여부를 포함하는 ContentInfo를 반환
     * 호율성을 위해 JSON 파싱은 한 번만 진행
     * @param step  파싱할 시나리오 스텝 엔티티
     * @return 파싱된 메타 정보(Optional)와 choices 존재 여부를 담은 ContentInfo 객체
     */
    public ContentInfo parseContentInfo(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());

            // meta 추출
            JsonNode metaNode = root.get("meta");
            StepMeta meta = (metaNode != null && !metaNode.isNull())
                    ? objectMapper.treeToValue(metaNode, StepMeta.class)
                    : null;
            Optional<StepMeta> metaOpt = Optional.ofNullable(meta);

            // choices 존재 여부 확인
            JsonNode choicesNode = root.get("choices");
            boolean hasChoices = choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0;

            return new ContentInfo(metaOpt, hasChoices);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝 content JSON 파싱 실패. stepId=" + step.getId()
            );
        }
    }
}
