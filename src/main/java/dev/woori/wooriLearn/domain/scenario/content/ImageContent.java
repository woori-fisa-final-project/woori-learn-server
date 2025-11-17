package dev.woori.wooriLearn.domain.scenario.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Image 타입 스텝의 content 전체를 표현하는 DTO
 * @param meta  해당 스텝의 메타 정보
 * @param image 렌더링에 사용할 이미지 URL
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageContent(
        StepMeta meta,
        String image
) {}
