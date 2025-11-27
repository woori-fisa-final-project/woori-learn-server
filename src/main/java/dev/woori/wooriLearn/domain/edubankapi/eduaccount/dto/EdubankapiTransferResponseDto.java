package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record EdubankapiTransferResponseDto(
        String transactionId,          // 거래 고유 ID
        LocalDateTime transactionDate, // 거래 시각
        String counterpartyName,       // 거래 상대방 이름 (자동 조회)
        Integer amount,                // 이체 금액
        Integer balance,               // 거래 후 잔액
        String message,                // 처리 결과 메시지
        String formattedAccountNumber  // 하이픈 포함된 계좌번호 추가 필드
) {
    /**
     * 계좌번호 포맷 함수
     * - DB에는 하이픈 없이 저장
     * - 응답에서는 1002-166-728332 형태로 반환
     */
    public static String formatAccountNumber(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", ""); // 숫자만 남기기
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 6) return digits.substring(0, 3) + "-" + digits.substring(3);
        return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
    }

    /**
     * 정적 팩토리 메서드
     * 서비스 레벨에서 계좌번호를 받아 포맷된 응답 생성
     */
    public static EdubankapiTransferResponseDto of(
            String transactionId,
            LocalDateTime transactionDate,
            String counterpartyName,
            Integer amount,
            Integer balance,
            String message,
            String rawAccountNumber
    ) {
        return EdubankapiTransferResponseDto.builder()
                .transactionId(transactionId)
                .transactionDate(transactionDate)
                .counterpartyName(counterpartyName)
                .amount(amount)
                .balance(balance)
                .message(message)
                .formattedAccountNumber(formatAccountNumber(rawAccountNumber)) // 자동 포맷
                .build();
    }
}
