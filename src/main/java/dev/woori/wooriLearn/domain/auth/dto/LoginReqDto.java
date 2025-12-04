package dev.woori.wooriLearn.domain.auth.dto;

import dev.woori.wooriLearn.domain.auth.dto.util.ValidPassword;
import dev.woori.wooriLearn.domain.auth.dto.util.ValidUserId;

public record LoginReqDto(
        @ValidUserId
        String userId,

        @ValidPassword
        String password
) {
}
