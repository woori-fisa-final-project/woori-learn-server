package dev.woori.wooriLearn.domain.account.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountAuthDtoTest {

    static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("하이픈이 있어도 정규화 후 통과(전화번호/생년월일)")
    void request_valid_afterNormalization() {
        // builder는 setter를 타지 않으므로, 정규화 검증용으로 setter 직접 호출
        AccountAuthDto.Request req = new AccountAuthDto.Request();
        req.setName("김철수");
        req.setPhoneNum("010-2222-2222");
        req.setBirthdate("04-01-01-3");

        // validate
        Set<ConstraintViolation<AccountAuthDto.Request>> v = validator.validate(req);
        assertThat(v).isEmpty();

        // 정규화 결과 확인
        assertThat(req.getPhoneNum()).isEqualTo("01022222222");
        assertThat(req.getBirthdate()).isEqualTo("0401013");
    }

    @Test
    @DisplayName("전화번호가 01로 시작하지 않으면 실패")
    void request_invalid_phone_prefix() {
        AccountAuthDto.Request req = new AccountAuthDto.Request();
        req.setName("김철수");
        req.setPhoneNum("02-1234-5678");
        req.setBirthdate("0401013");

        Set<ConstraintViolation<AccountAuthDto.Request>> v = validator.validate(req);
        assertThat(v).isNotEmpty();
        // phoneNum 관련 제약 하나 이상 존재해야 함
        assertThat(v.stream().anyMatch(cv -> "phoneNum".equals(cv.getPropertyPath().toString()))).isTrue();
    }

    @Test
    @DisplayName("생년월일 7자리가 아니면 실패")
    void request_invalid_birth_pattern_length() {
        AccountAuthDto.Request req = new AccountAuthDto.Request();
        req.setName("김철수");
        req.setPhoneNum("01022223333");
        req.setBirthdate("040101");

        Set<ConstraintViolation<AccountAuthDto.Request>> v = validator.validate(req);
        assertThat(v).isNotEmpty();
        assertThat(v.stream().anyMatch(cv -> "birthdate".equals(cv.getPropertyPath().toString()))).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 날짜면 실패")
    void request_invalid_birth_impossible_date() {
        AccountAuthDto.Request req = new AccountAuthDto.Request();
        req.setName("김철수");
        req.setPhoneNum("01022223333");
        req.setBirthdate("0433313");

        Set<ConstraintViolation<AccountAuthDto.Request>> v = validator.validate(req);
        assertThat(v).isNotEmpty();
    }

    @Test
    @DisplayName("만 14세 미만이면 실패")
    void request_invalid_birth_too_young() {
        AccountAuthDto.Request req = new AccountAuthDto.Request();
        req.setName("김철수");
        req.setPhoneNum("01022223333");
        req.setBirthdate("2402013");

        Set<ConstraintViolation<AccountAuthDto.Request>> v = validator.validate(req);
        assertThat(v).isNotEmpty();
    }

    @Test
    @DisplayName("인증번호 6자리 숫자만 허용")
    void verifyRequest_code_pattern() {
        AccountAuthDto.VerifyRequest ok = AccountAuthDto.VerifyRequest.builder().code("123456").build();
        AccountAuthDto.VerifyRequest shortCode = AccountAuthDto.VerifyRequest.builder().code("12345").build();
        AccountAuthDto.VerifyRequest nonDigit = AccountAuthDto.VerifyRequest.builder().code("12a456").build();

        assertThat(validator.validate(ok)).isEmpty();
        assertThat(validator.validate(shortCode)).isNotEmpty();
        assertThat(validator.validate(nonDigit)).isNotEmpty();
    }
}