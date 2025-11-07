package dev.woori.wooriLearn.domain.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 인증번호 검증 요청 DTO
 */
public record AccountAuthVerifyReqDto(
        @NotBlank(message = "인증번호를 입력해주세요.")
        @Pattern(regexp = "^\\d{6}$")
        String code
) {}
