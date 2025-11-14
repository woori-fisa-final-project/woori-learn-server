package dev.woori.wooriLearn.domain.user.dto;

import lombok.Builder;

@Builder
public record UserInfoResDto(
    String nickname,
    int point
) {
}
