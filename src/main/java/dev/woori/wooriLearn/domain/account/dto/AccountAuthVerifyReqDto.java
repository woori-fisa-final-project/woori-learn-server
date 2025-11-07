package dev.woori.wooriLearn.domain.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * 인증번호 검증 요청
 * - 6자리 숫자만 허용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountAuthVerifyReqDto {
    @NotBlank(message = "인증번호를 입력해주세요.")
    @Pattern(regexp = "^\\d{6}$")
    private String code;
}
