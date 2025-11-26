package dev.woori.wooriLearn.domain.account.dto.response;

import dev.woori.wooriLearn.domain.account.dto.AccountInfo;

public record AccountCreateResDto(
        String code,
        String message,
        AccountInfo data
) {
}
