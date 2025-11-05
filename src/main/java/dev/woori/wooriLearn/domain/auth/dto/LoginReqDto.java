package dev.woori.wooriLearn.domain.auth.dto;

import lombok.Builder;

@Builder
public record LoginReqDto(
    String userId,
    String password
) {
}
