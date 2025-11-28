package dev.woori.wooriLearn.domain.edubankapi.autopayment.dto;

/**
 * 자동이체 일괄 해지 응답 DTO
 *
 * 타입 안정성과 API 명세의 명확성을 위해 Map 대신 명시적인 DTO 사용
 *
 * @param cancelledCount 해지된 자동이체 건수
 * @param message 응답 메시지
 */
public record CancelAllAutoPaymentsResponse(
        int cancelledCount,
        String message
) {
    /**
     * 정적 팩토리 메서드 - 해지 건수로부터 응답 생성
     *
     * 0건일 경우 사용자에게 더 명확한 메시지를 전달
     *
     * @param count 해지된 자동이체 건수
     * @return CancelAllAutoPaymentsResponse
     */
    public static CancelAllAutoPaymentsResponse of(int count) {
        String message = (count == 0)
                ? "해지할 활성 자동이체가 없습니다."
                : count + "건의 자동이체가 해지되었습니다.";

        return new CancelAllAutoPaymentsResponse(count, message);
    }
}
