package dev.woori.wooriLearn.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.woori.wooriLearn.validation.ValidBirthdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * 계좌 본인인증(OTP) 플로우에서 사용하는 요청/응답 DTO 모음.
 * - Request: 인증번호 발급 요청 바디 (이름/전화/생년월일)
 * - Response: 발급 요청에 대한 서버 응답(메시지)
 * - VerifyRequest: 인증번호 검증 요청 바디(6자리 코드)
 * - VerifyResponse: 검증 결과(성공/실패)
 */

public class AccountAuthDto {

    /**
     * 인증번호 발급 요청 바디.
     * 서버는 이 값들을 저장하지 않고, 외부 인증 서버로만 전달한다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
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

        @NotBlank
        @ValidBirthdate(minAgeYears = 14)
        @Pattern(regexp="^\\d{7}$", message="생년월일은 YYMMDDG 형식(하이픈 없이 7자리)이어야 합니다.")
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

    /**
     * 인증번호 발급 요청에 대한 응답.
     * OTP 숫자 노출 방지를 위해 "SENT"로 반환
     */
    @Getter
    @AllArgsConstructor
    public static class Response {
        private String message;
    }

    /**
     * 인증번호 검증 요청 바디.
     * - 6자리 숫자만 허용
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyRequest {
        @Pattern(regexp = "^\\d{6}$")
        private String code;
    }

    /**
     * 인증번호 검증 결과.
     * - verified=true 이면 인증 성공(해당 레코드 삭제)
     * - verified=false 이면 불일치 실패
     */
    @Getter
    @AllArgsConstructor
    public static class VerifyResponse {
        private boolean verified;
    }
}
