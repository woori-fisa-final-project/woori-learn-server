package dev.woori.wooriLearn.domain.account.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountCreateReqDto(
        @NotBlank String userId,
        @NotBlank String accountNum,
        @NotBlank String name
) {
}
