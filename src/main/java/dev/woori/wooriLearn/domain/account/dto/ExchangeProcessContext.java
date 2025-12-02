package dev.woori.wooriLearn.domain.account.dto;

import lombok.Builder;

@Builder
public record ExchangeProcessContext(
    Long requestId,
    Long userId,
    String accountNum,
    long amount
) {
}
