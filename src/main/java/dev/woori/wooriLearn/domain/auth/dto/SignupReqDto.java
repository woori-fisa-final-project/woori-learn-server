package dev.woori.wooriLearn.domain.auth.dto;

public record SignupReqDto(
    String userId,
    String password,
    String nickname
) {
}
