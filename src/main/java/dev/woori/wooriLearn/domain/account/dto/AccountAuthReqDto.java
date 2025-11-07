package dev.woori.wooriLearn.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.woori.wooriLearn.validation.ValidBirthdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * 인증번호 발급 요청 바디.
 * 서버는 이 값들을 저장하지 않고, 외부 인증 서버로만 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountAuthReqDto {

    @NotBlank
    private String name;

    /**
     * 전화번호.
     * - 입력 키는 "phone num" / "phone_num" / "phoneNum" 모두 허용
     *   (JsonProperty + JsonAlias 로 다양한 클라이언트 키를 수용)
     * - 서버에서는 하이픈 등 비숫자를 모두 제거(setter에서 정규화)
     * - 최종 검증은 숫자만: ^01\\d{8,9}$ (예: 01022222222)
     */
    @NotBlank
    @JsonProperty("phone num")
    @JsonAlias({"phone_num", "phoneNum"})
    @Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호는 하이픈 없이 숫자만 입력하세요.")
    private String phoneNum;

    @ValidBirthdate(minAgeYears = 14)
    private String birthdate;

    // 클라이언트가 하이픈/공백 등을 섞어 보내더라도 서버에서 비숫자를 제거하여 숫자만 남김.
    public void setPhoneNum(String phoneNum) {
        this.phoneNum = (phoneNum == null) ? null : phoneNum.replaceAll("\\D", "");
    }

    // 클라이언트가 하이픈/공백 등을 섞어 보내더라도 서버에서 비숫자를 제거하여 숫자만 남김.
    public void setBirthdate(String birthdate) {
        this.birthdate = (birthdate == null) ? null : birthdate.replaceAll("\\D", "");
    }
}
