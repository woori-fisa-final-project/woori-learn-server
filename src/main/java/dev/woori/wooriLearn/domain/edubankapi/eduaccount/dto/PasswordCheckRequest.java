package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

public record PasswordCheckRequest(
        String accountNumber,
        String password
) {}