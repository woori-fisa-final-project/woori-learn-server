package dev.woori.wooriLearn.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 시나리오 진행(체크포인트) 저장 요청 DTO
 * 사용자가 현재 머물러 있는 스텝을 저장할 때 사용
 *
 * @param nowStepId 현재 사용자가 위치한 스텝 ID
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProgressSaveReqDto(
        @NotNull
        @Positive
        Long nowStepId
) {}
