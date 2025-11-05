package dev.woori.wooriLearn.domain.auth.dto;

import lombok.Builder;

@Builder
public record RefreshReqDto(
        String refreshToken
) {
}
