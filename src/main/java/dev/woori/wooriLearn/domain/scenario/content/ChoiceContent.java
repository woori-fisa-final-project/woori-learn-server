package dev.woori.wooriLearn.domain.scenario.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * CHOICE 타입 스텝의 content JSON을 매핑하는 DTO
 * @param choices 사용자가 선택할 수 있는 선택지 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoiceContent(
        List<ChoiceOption> choices
) {}
