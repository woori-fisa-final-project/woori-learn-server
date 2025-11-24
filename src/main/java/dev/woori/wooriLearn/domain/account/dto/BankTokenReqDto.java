package dev.woori.wooriLearn.domain.account.dto;

import lombok.Builder;

@Builder
public record BankTokenReqDto(
        String userId
) {
}
