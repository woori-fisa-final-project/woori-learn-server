package dev.woori.wooriLearn.domain.user.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserAccountDto(
        String accountNumber,
        LocalDateTime linkedAt
) {
}
