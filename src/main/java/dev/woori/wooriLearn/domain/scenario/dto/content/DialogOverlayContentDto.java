package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DIALOG/OVERLAY 전체 content
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DialogOverlayContentDto(
        StepMetaDto meta,
        List<DialogOverlayLineDto> dialogs
) {}
