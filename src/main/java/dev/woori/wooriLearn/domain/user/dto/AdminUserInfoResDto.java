package dev.woori.wooriLearn.domain.user.dto;

import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistoryDto;

import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioProgressResDto;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminUserInfoResDto(
        Long id,
        String userId,
        String nickname,
        int points,
        int exchangedPoints,
        Role role,
        LocalDateTime createdAt,
        double progressRate,
        UserAccountDto account,
        List<ScenarioProgressResDto> scenarios,
        List<PointsHistoryDto> pointHistories
) {
}
