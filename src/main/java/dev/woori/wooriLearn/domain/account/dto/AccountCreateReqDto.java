package dev.woori.wooriLearn.domain.account.dto;

public record AccountCreateReqDto(
        String userId,
        String accountNum,
        String name
) {
}
