package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DIALOG/OVERLAY 에서 한 줄 대사
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DialogOverlayLineDto(
        String text,
        String character,
        String image
) {}
