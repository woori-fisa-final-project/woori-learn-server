package dev.woori.wooriLearn.domain.account.dto.external.response;

import dev.woori.wooriLearn.domain.account.dto.SessionIdData;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ExternalAccountUrlResDto(
        @NotBlank String code,
        @NotBlank String message,
        @NotBlank SessionIdData data
) {
}
