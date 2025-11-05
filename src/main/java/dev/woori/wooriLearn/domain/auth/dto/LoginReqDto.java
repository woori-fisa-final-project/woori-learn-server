package dev.woori.wooriLearn.domain.auth.dto;

public record LoginReqDto(
    String userId,
    String password
) {
}
