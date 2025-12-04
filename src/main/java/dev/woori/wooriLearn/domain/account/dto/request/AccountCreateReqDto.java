package dev.woori.wooriLearn.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AccountCreateReqDto(
        @NotBlank String tid
) {
}
