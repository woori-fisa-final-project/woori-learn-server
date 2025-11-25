package dev.woori.wooriLearn.domain.scenario.dto;

/**
 * 시나리오 보상(포인트) 지급 결과 DTO
 *
 * @param rewarded 지급되었는지 여부
 * @param message  사용자 표시용 메시지
 */
public record ScenarioRewardResDto(
        boolean rewarded,
        String message
) {
}

