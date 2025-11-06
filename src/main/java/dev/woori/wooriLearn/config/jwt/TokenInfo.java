package dev.woori.wooriLearn.config.jwt;

import java.time.Instant;

public record TokenInfo(
        String token,
        Instant expiration
) {
}
