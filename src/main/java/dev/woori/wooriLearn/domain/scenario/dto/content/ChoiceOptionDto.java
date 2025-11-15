package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoiceOptionDto(
        Boolean good,
        Long next,
        String text
) {}

