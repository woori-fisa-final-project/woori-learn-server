package dev.woori.wooriLearn.domain.auth.dto.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIdValidatorTest {

    private final UserIdValidator validator = new UserIdValidator();

    @Test
    @DisplayName("영문 시작 + 숫자 포함 5자 이상의 아이디는 유효하다")
    void validUserIds() {
        assertTrue(validator.isValid("a1abc", null));
        assertTrue(validator.isValid("abcde1", null));
    }

    @Test
    @DisplayName("null, 숫자 시작, 특수문자 포함 등 규칙 위반 아이디는 무효이다")
    void invalidUserIds() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("1abcd", null)); // must start with letter
        assertFalse(validator.isValid("abc", null)); // too short
        assertFalse(validator.isValid("abcdef", null)); // missing digit
        assertFalse(validator.isValid("abcde!", null)); // invalid char
    }
}
