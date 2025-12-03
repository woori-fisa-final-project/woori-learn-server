package dev.woori.wooriLearn.domain.user.service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountNumberGeneratorTest {

    @Test
    void generateNumeric_returnsDigitsWithRequestedLength() {
        int len = 12;
        String number = AccountNumberGenerator.generateNumeric(len);
        assertEquals(len, number.length());
        assertTrue(number.matches("\\d{" + len + "}"));
    }
}
