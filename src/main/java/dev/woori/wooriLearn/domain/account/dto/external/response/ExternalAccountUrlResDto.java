package dev.woori.wooriLearn.domain.account.dto.external.response;

import dev.woori.wooriLearn.domain.account.dto.SessionIdData;
import lombok.Builder;

@Builder
public record ExternalAccountUrlResDto(
        String code,
        String message,
        SessionIdData data
) {
}
