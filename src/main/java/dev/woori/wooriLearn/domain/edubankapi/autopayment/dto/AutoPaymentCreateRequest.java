package dev.woori.wooriLearn.domain.edubankapi.autopayment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.validation.ValidDesignatedDate;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * 자동이체 등록 요청 DTO
 *
 * 지정일 처리 정책:
 * - 1~31일: 해당 일자에 이체 (해당 월에 없으면 말일에 실행)
 * - 99: 매월 말일에 이체 (특수값)
 * - 예시:
 * · 31일 지정 → 2월: 28일(윤년 29일), 4월/6월/9월/11월: 30일
 * · 99 지정 → 1월: 31일, 2월: 28일(윤년 29일), 4월: 30일
 */
public record AutoPaymentCreateRequest(
        @NotNull(message = "교육용 계좌 ID는 필수입니다.")
        @Positive(message = "교육용 계좌 ID는 양수여야 합니다.")
        Long educationalAccountId,

        @NotBlank(message = "입금 은행 코드는 필수입니다.")
        @Size(max = 10, message = "입금 은행 코드는 10자 이하여야 합니다.")
        String depositBankCode,

        @NotBlank(message = "입금 계좌번호는 필수입니다.")
        @Size(max = 20, message = "입금 계좌번호는 20자 이하여야 합니다.")
        String depositNumber,

        @NotNull(message = "이체 금액은 필수입니다.")
        @Positive(message = "이체 금액은 양수여야 합니다.")
        Integer amount,

        @NotBlank(message = "상대방 이름은 필수입니다.")
        @Size(max = 30, message = "상대방 이름은 30자 이하여야 합니다.")
        String counterpartyName,

        @NotBlank(message = "표시 이름은 필수입니다.")
        @Size(max = 30, message = "표시 이름은 30자 이하여야 합니다.")
        String displayName,

        @NotNull(message = "이체 주기는 필수입니다.")
        @Positive(message = "이체 주기는 양수여야 합니다.")
        Integer transferCycle,

        @NotNull(message = "지정일은 필수입니다.")
        @ValidDesignatedDate
        Integer designatedDate,

        @NotNull(message = "시작일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull(message = "만료일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate expirationDate,

        @NotBlank(message = "계좌 비밀번호는 필수입니다.")
        @Size(min = 4, max = 4, message = "계좌 비밀번호는 4자리여야 합니다.")
        String accountPassword
) {
    /**
     * 만료일 유효성 검증
     */
    @AssertTrue(message = "만료일은 시작일 이후여야 합니다.")
    private boolean isExpirationDateValid() {
        // null 체크: DTO 단독 validation 시 NPE 방지
        if (startDate == null || expirationDate == null) {
            return true;  // null 검증은 @NotNull이 담당
        }
        return !expirationDate.isBefore(startDate);
    }

    @Override
    public String toString() {
        return "AutoPaymentCreateRequest[" +
                "educationalAccountId=" + educationalAccountId +
                ", depositBankCode='" + depositBankCode + '\'' +
                ", depositNumber='" + depositNumber + '\'' +
                ", amount=" + amount +
                ", counterpartyName='" + counterpartyName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", transferCycle=" + transferCycle +
                ", designatedDate=" + designatedDate +
                ", startDate=" + startDate +
                ", expirationDate=" + expirationDate +
                ", accountPassword='****'" +
                ']';
    }

}