package dev.woori.wooriLearn.domain.user.dto;

import dev.woori.wooriLearn.domain.auth.entity.Role;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminUserListResDto(
        Long id,
        String userId,
        String nickname,
        int points,
        Role role,
        LocalDateTime createdAt,
        double progressRate
) {
}
