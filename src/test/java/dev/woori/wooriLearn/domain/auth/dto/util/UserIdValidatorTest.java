package dev.woori.wooriLearn.domain.auth.dto.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIdValidatorTest {

    private final UserIdValidator validator = new UserIdValidator();

    @Test
    void validUserIds() {
        assertTrue(validator.isValid("a1abc", null));
        assertTrue(validator.isValid("abcde1", null));
    }

    @Test
    void invalidUserIds() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("1abcd", null)); // must start with letter
        assertFalse(validator.isValid("abc", null)); // too short
        assertFalse(validator.isValid("abcdef", null)); // missing digit
        assertFalse(validator.isValid("abcde!", null)); // invalid char
    }
}
