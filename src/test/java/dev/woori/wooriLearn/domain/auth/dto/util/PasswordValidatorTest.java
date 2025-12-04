package dev.woori.wooriLearn.domain.auth.dto.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    @DisplayName("대소문자+숫자+특수문자를 포함하면 비밀번호가 유효하다")
    void validPasswords() {
        assertTrue(validator.isValid("Abcd1234!", null));
        assertTrue(validator.isValid("A1!aaaaa", null));
    }

    @Test
    @DisplayName("null, 길이 부족, 조합 미충족 비밀번호는 무효이다")
    void invalidPasswords() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("short1!", null)); // too short
        assertFalse(validator.isValid("alllowercase1", null)); // missing special
        assertFalse(validator.isValid("ALLUPPER!@", null)); // missing digit
    }
}
