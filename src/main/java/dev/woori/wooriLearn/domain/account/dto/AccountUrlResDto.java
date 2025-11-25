package dev.woori.wooriLearn.domain.account.dto;

import lombok.Builder;

@Builder
public record AccountUrlResDto(
        String accessToken,
        String url
) {
}
