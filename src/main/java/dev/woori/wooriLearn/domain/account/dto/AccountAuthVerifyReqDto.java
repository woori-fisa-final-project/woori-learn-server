package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.AccountAuth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 인증번호 검증 요청 DTO
 */
public record AccountAuthVerifyReqDto(

        @NotBlank(message = "인증번호를 입력해주세요.")
        @Pattern(regexp = AccountAuth.AUTH_CODE_REGEX, message = "인증번호는 6자리 숫자여야 합니다.")
        String code
) {}
