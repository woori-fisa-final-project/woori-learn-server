package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PointsExchangeResponseDto(
        Long requestId,
        Long userId,
        Integer exchangeAmount,
        PointsStatus status,
        String requestDate,
        String message,
        LocalDateTime processedDate
) {}
