package dev.woori.wooriLearn.domain.scenario.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * PRACTICE 타입 스텝의 content 전체를 표현하는 DTO
 * @param meta  해당 스텝의 메타 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PracticeContent(
        StepMeta meta
) {}
