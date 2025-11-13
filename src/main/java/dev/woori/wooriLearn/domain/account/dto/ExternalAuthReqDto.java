package dev.woori.wooriLearn.domain.account.dto;

public record ExternalAuthReqDto(
        String name,
        String birthdate,
        String phone
) {}
