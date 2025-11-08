package dev.woori.wooriLearn.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.woori.wooriLearn.validation.ValidBirthdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 인증번호 발급 요청 DTO
 * - phoneNum, birthdate는 생성자에서 비숫자 제거 후 검증.
 */
public record AccountAuthReqDto(

        @NotBlank
        String name,

        @NotBlank
        @JsonProperty("phone num")
        @JsonAlias({ "phone_num", "phoneNum" })
        @Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호는 하이픈 없이 숫자만 입력하세요.")
        String phoneNum,

        @ValidBirthdate(minAgeYears = 14)
        String birthdate
) {
    public AccountAuthReqDto {
        phoneNum  = sanitize(phoneNum);
        birthdate = sanitize(birthdate);
    }

    private static String sanitize(String input) {
        return (input == null) ? null : input.replaceAll("\\D", "");
    }
}
