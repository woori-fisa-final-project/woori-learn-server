package dev.woori.wooriLearn.config.jwt;

import dev.woori.wooriLearn.domain.auth.entity.Role;

public record JwtInfo(
        String username,
        Role role
) {
}
