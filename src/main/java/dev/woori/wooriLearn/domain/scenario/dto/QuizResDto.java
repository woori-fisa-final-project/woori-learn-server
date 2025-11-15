package dev.woori.wooriLearn.domain.scenario.dto;

import java.util.List;

/**
 * 시나리오 퀴즈 응답 DTO
 *
 * @param id        퀴즈 ID
 * @param question  퀴즈 질문
 * @param options   보기 목록
 */
public record QuizResDto(
        Long id,
        String question,
        List<String> options
) {}
