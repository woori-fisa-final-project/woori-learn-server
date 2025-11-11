package dev.woori.wooriLearn.domain.edubankapi.autopayment.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("자동이체 등록 요청 DTO 검증 테스트")
class AutoPaymentCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("정상 요청 - 모든 필드 유효")
    void validate_Success_AllFieldsValid() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L,
                "020",
                "110-123-456789",
                50000,
                "홍길동",
                "월세",
                1,
                5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("정상 요청 - 시작일과 만료일이 같은 날")
    void validate_Success_SameDate() {
        // given
        LocalDate sameDate = LocalDate.of(2025, 1, 1);
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                sameDate,
                sameDate,  // 같은 날
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("검증 실패 - educationalAccountId null")
    void validate_Fail_NullEducationalAccountId() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                null,  // educationalAccountId
                "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("교육용 계좌 ID는 필수입니다.");
    }

    @Test
    @DisplayName("검증 실패 - educationalAccountId 음수")
    void validate_Fail_NegativeEducationalAccountId() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                -1L,  // 음수
                "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("교육용 계좌 ID는 양수여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - amount 음수")
    void validate_Fail_NegativeAmount() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789",
                -1000,  // 음수
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("이체 금액은 양수여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - designatedDate 범위 초과 (32일)")
    void validate_Fail_DesignatedDateTooLarge() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1,
                32,  // 31 초과
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("지정일은 31일 이하여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - designatedDate 범위 미만 (0일)")
    void validate_Fail_DesignatedDateTooSmall() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1,
                0,  // 1 미만
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("지정일은 1일 이상이어야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - accountPassword 길이 부족 (3자리)")
    void validate_Fail_PasswordTooShort() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "123"  // 3자리
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("계좌 비밀번호는 4자리여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - accountPassword 길이 초과 (5자리)")
    void validate_Fail_PasswordTooLong() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "12345"  // 5자리
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("계좌 비밀번호는 4자리여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - startDate null")
    void validate_Fail_NullStartDate() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                null,  // startDate
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("시작일은 필수입니다.");
    }

    @Test
    @DisplayName("검증 실패 - expirationDate null")
    void validate_Fail_NullExpirationDate() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                null,  // expirationDate
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("만료일은 필수입니다.");
    }

    @Test
    @DisplayName("검증 실패 - 만료일이 시작일보다 이전")
    void validate_Fail_ExpirationDateBeforeStartDate() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 12, 31),  // 시작일
                LocalDate.of(2025, 1, 1),    // 만료일 (이전!)
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("만료일은 시작일 이후여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - 여러 필드 동시 위반")
    void validate_Fail_MultipleViolations() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                null,     // educationalAccountId null
                "",       // depositBankCode empty
                "110-123-456789",
                -1000,    // amount 음수
                "홍길동",
                "월세",
                1,
                32,       // designatedDate 범위 초과
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 1, 1),  // 만료일이 시작일보다 이전
                "12345"   // password 길이 초과
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(5);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "교육용 계좌 ID는 필수입니다.",
                        "입금 은행 코드는 필수입니다.",
                        "이체 금액은 양수여야 합니다.",
                        "지정일은 31일 이하여야 합니다.",
                        "만료일은 시작일 이후여야 합니다.",
                        "계좌 비밀번호는 4자리여야 합니다."
                );
    }

    @Test
    @DisplayName("검증 실패 - depositBankCode 길이 초과")
    void validate_Fail_DepositBankCodeTooLong() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L,
                "12345678901",  // 11자 (10자 초과)
                "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("입금 은행 코드는 10자 이하여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - depositNumber 길이 초과")
    void validate_Fail_DepositNumberTooLong() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020",
                "123456789012345678901",  // 21자 (20자 초과)
                50000, "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("입금 계좌번호는 20자 이하여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - counterpartyName 길이 초과")
    void validate_Fail_CounterpartyNameTooLong() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "가".repeat(31),  // 31자 (30자 초과)
                "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("상대방 이름은 30자 이하여야 합니다.");
    }

    @Test
    @DisplayName("검증 실패 - displayName 길이 초과")
    void validate_Fail_DisplayNameTooLong() {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동",
                "가".repeat(31),  // 31자 (30자 초과)
                1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        // when
        Set<ConstraintViolation<AutoPaymentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("표시 이름은 30자 이하여야 합니다.");
    }
}