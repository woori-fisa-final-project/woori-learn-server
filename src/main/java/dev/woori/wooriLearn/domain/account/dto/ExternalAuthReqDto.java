package dev.woori.wooriLearn.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExternalAuthReqDto(
        String name,
        String birthdate,
        String phone
) {}
