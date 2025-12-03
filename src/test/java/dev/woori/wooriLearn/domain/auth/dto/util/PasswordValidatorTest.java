package dev.woori.wooriLearn.domain.auth.dto.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void validPasswords() {
        assertTrue(validator.isValid("Abcd1234!", null));
        assertTrue(validator.isValid("A1!aaaaa", null));
    }

    @Test
    void invalidPasswords() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("short1!", null)); // too short
        assertFalse(validator.isValid("alllowercase1", null)); // missing special
        assertFalse(validator.isValid("ALLUPPER!@", null)); // missing digit
    }
}
