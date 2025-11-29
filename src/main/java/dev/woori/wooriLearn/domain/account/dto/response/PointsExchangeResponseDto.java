package dev.woori.wooriLearn.domain.account.dto.response;

import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PointsExchangeResponseDto(
        Long requestId,
        Long userId,
        Integer exchangeAmount,
        Integer currentBalance,
        PointsStatus status,
        LocalDateTime  requestDate,
        String message,
        LocalDateTime processedDate
) {
    public static PointsExchangeResponseDto from(PointsHistory h) {
        return PointsExchangeResponseDto.builder()
                .requestId(h.getId())
                .userId(h.getUser().getId())
                .exchangeAmount(h.getAmount())
                .currentBalance(h.getUser().getPoints())
                .status(h.getStatus())
                .requestDate(h.getCreatedAt())
                .processedDate(h.getProcessedAt())
                .message(h.getStatus().message())
                .build();
}
}
