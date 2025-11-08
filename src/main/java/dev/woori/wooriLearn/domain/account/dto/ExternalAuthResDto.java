package dev.woori.wooriLearn.domain.account.dto;

/** 외부 인증 서버에서 내려주는 응답 바디 */
public record ExternalAuthResDto(
        String code
) {}
