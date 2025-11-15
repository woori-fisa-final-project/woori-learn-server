package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.dto.content.ChoiceContentDto;
import dev.woori.wooriLearn.domain.scenario.dto.content.ChoiceOptionDto;
import dev.woori.wooriLearn.domain.scenario.dto.content.DialogOverlayContentDto;
import dev.woori.wooriLearn.domain.scenario.dto.content.ImageContentDto;
import dev.woori.wooriLearn.domain.scenario.dto.content.ModalContentDto;
import dev.woori.wooriLearn.domain.scenario.dto.content.StepMetaDto;
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.ChoiceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScenarioStepContentService {

    private final ObjectMapper objectMapper;

    /** ScenarioStep -> ProgressResumeResDto 로 매핑 (콘텐츠 JsonNode 포함) */
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

    /** CHOICE 스텝 content -> ChoiceInfo 로 파싱 */
    public ChoiceInfo parseChoice(ScenarioStep step, int answerIndex) {
        try {
            ChoiceContentDto content = objectMapper.readValue(
                    step.getContent(),
                    ChoiceContentDto.class
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

            ChoiceOptionDto selected = content.choices().get(answerIndex);
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

    /** 배드 브랜치 여부(meta.branch == "bad") */
    public boolean isBadBranch(ScenarioStep step) {
        return getMeta(step)
                .map(meta -> "bad".equalsIgnoreCase(meta.branch()))
                .orElse(false);
    }

    /** 배드 엔딩 여부(meta.badEnding == true) */
    public boolean isBadEnding(ScenarioStep step) {
        return getMeta(step)
                .map(meta -> Boolean.TRUE.equals(meta.badEnding()))
                .orElse(false);
    }

    /** 각 StepType 에 맞는 DTO로 content 파싱 후 meta 추출 */
    public Optional<StepMetaDto> getMeta(ScenarioStep step) {
        try {
            return switch (step.getType()) {
                case CHOICE -> Optional.empty(); // CHOICE에는 meta 사용 안 함

                case DIALOG, OVERLAY -> {
                    DialogOverlayContentDto content = objectMapper.readValue(
                            step.getContent(),
                            DialogOverlayContentDto.class
                    );
                    yield Optional.ofNullable(content.meta());
                }

                case IMAGE -> {
                    ImageContentDto content = objectMapper.readValue(
                            step.getContent(),
                            ImageContentDto.class
                    );
                    yield Optional.ofNullable(content.meta());
                }

                case MODAL -> {
                    ModalContentDto content = objectMapper.readValue(
                            step.getContent(),
                            ModalContentDto.class
                    );
                    yield Optional.ofNullable(content.meta());
                }
            };
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝 content JSON 파싱 실패. stepId=" + step.getId()
            );
        }
    }
}
