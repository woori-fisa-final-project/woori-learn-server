package dev.woori.wooriLearn.domain.user.dto;

import dev.woori.wooriLearn.domain.auth.dto.util.ValidPassword;
import dev.woori.wooriLearn.domain.auth.dto.util.ValidUserId;
import jakarta.validation.constraints.NotBlank;

public record SignupReqDto(
        @ValidUserId
        String userId,

        @ValidPassword
        String password,

        @NotBlank
        String nickname,

        @NotBlank
        String email
) {
}
