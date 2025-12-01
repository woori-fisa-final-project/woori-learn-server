package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordCheckRequest(
        @NotBlank(message = "계좌번호는 필수입니다.")
        String accountNumber,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}