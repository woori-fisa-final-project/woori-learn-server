package dev.woori.wooriLearn.domain.auth.dto;

import dev.woori.wooriLearn.domain.auth.dto.util.ValidPassword;

public record ChangePasswdReqDto(
        String currentPassword,

        @ValidPassword
        String newPassword
) {
}
