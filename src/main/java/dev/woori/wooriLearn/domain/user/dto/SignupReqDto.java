package dev.woori.wooriLearn.domain.user.dto;

public record SignupReqDto(
    String userId,
    String password,
    String nickname
) {
}
