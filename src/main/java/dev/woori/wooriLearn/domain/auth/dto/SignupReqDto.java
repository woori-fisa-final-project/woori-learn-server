package dev.woori.wooriLearn.domain.auth.dto;

import lombok.Builder;

@Builder
public record SignupReqDto(
    String userId,
    String password,
    String nickname
) {
}
