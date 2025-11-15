package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CHOICE 스텝의 각 선택지 하나를 표현하는 DTO
 * @param good 사용자의 선택이 정루트인지 여부
 * @param next 해당 선택지를 탔을 때 이동할 다음 스텝의 ID
 * @param text 사용자에게 보여줄 선택지 문구
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoiceOptionDto(
        Boolean good,
        Long next,
        String text
) {}

