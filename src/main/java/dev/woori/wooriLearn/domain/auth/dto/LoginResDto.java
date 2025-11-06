package dev.woori.wooriLearn.domain.auth.dto;

public record LoginResDto(
        String accessToken,
        String refreshToken
) {
}
