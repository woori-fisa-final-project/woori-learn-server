package dev.woori.wooriLearn.domain.auth.dto;

public record ChangePasswdReqDto(
        String currentPassword,
        String newPassword
) {
}
