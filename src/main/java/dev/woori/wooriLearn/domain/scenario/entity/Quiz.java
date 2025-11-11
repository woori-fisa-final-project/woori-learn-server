package dev.woori.wooriLearn.domain.scenario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    // ---- Simple validations ----
    private static void validateQuestion(String q) {
        if (q == null || q.isBlank())
            throw new IllegalArgumentException("question은 비어 있을 수 없습니다.");
    }

    private static void validateOptionsJson(String json) {
        if (json == null) throw new IllegalArgumentException("options는 null일 수 없습니다.");
        String t = json.trim();
        if (!(t.startsWith("[") && t.endsWith("]")))
            throw new IllegalArgumentException("options는 JSON 배열 문자열이어야 합니다.");
    }

    private static void validateAnswer(Integer a) {
        if (a == null) throw new IllegalArgumentException("answer는 null일 수 없습니다.");
    }
}
