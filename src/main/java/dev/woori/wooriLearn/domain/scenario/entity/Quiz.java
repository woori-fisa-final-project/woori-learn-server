package dev.woori.wooriLearn.domain.scenario.entity;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String options; // JSON 문자열

    @Column(nullable = false)
    private Integer answer;

    public static Quiz create(String question, String optionsJson, Integer answer) {
        validateQuestion(question);
        validateOptionsJson(optionsJson);
        validateAnswer(answer);
        return Quiz.builder()
                .question(question)
                .options(optionsJson)
                .answer(answer)
                .build();
    }

    public void changeQuestion(String newQuestion) {
        validateQuestion(newQuestion);
        this.question = newQuestion;
    }

    public void changeOptionsJson(String newOptionsJson) {
        validateOptionsJson(newOptionsJson);
        this.options = newOptionsJson;
    }

    public void changeAnswer(Integer newAnswer) {
        validateAnswer(newAnswer);
        this.answer = newAnswer;
    }

    private static void validateQuestion(String q) {
        if (q == null || q.isBlank()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "question이 비어 있을 수 없습니다.");
        }
    }

    private static void validateOptionsJson(String json) {
        if (json == null) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "options가 null일 수 없습니다.");
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
            if (!node.isArray()) {
                throw new CommonException(ErrorCode.INVALID_REQUEST, "options는 JSON 배열 형식의 문자열이어야 합니다.");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "options가 유효한 JSON 형식이 아닙니다.");
        }
    }

    private static void validateAnswer(Integer a) {
        if (a == null) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "answer가 null일 수 없습니다.");
        }
    }
}
