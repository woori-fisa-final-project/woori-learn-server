package dev.woori.wooriLearn.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 시나리오 진행 요청 DTO
 *
 * @param nowStepId 현재 사용자가 위치한 스텝
 * @param answer    퀴즈가 있는 스텝일 경우 사용자가 고른 보기 인덱스
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvanceReqDto(
        Long nowStepId,
        Integer answer
) {}
