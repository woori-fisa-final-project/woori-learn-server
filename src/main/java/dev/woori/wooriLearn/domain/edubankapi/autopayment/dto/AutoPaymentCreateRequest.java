package dev.woori.wooriLearn.domain.edubankapi.autopayment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

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
        @Min(value = 1, message = "지정일은 1일 이상이어야 합니다.")
        @Max(value = 31, message = "지정일은 31일 이하여야 합니다.")
        Integer designatedDate,

        @NotNull(message = "시작일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate expirationDate,

        @NotBlank(message = "계좌 비밀번호는 필수입니다.")
        @Size(min = 4, max = 4, message = "계좌 비밀번호는 4자리여야 합니다.")
        String accountPassword
) {
}