package dev.woori.wooriLearn.domain.account.dto;

import lombok.Builder;

@Builder
public record AccountCreateReqDto(
        String id,
        String code
) {
}
