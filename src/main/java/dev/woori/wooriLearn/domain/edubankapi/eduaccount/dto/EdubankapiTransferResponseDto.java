package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EdubankapiTransferResponseDto(
        String transactionId,                       // 거래 고유 ID (예: TX202511101230)
        LocalDateTime transactionDate,              // 거래가 발생한 실제 시각
        String counterpartyName,                    // 거래 상대방 이름
        Integer amount,                             // 이체 금액
        Integer balance,                            // 거래 후 잔액
        String message                              // 처리 결과 메시지
) {
}
