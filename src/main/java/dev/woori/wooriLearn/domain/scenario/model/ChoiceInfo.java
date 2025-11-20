package dev.woori.wooriLearn.domain.scenario.model;

/**
 * CHOICE 스텝에서 사용자가 선택한 하나의 선택지 정보를 담는 모델
 * @param good          해당 선택지가 정루트인지 여부
 * @param nextStepId    이동할 다음 스텝 ID
 */
public record ChoiceInfo(
        boolean good,
        Long nextStepId
) {}
