package dev.woori.wooriLearn.domain.user.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountNumberGeneratorTest {

    // ASCII-only comment to keep source encoding stable.

    @Test
    @DisplayName("요청한 길이만큼 숫자 문자열을 생성한다")
    void generateNumeric_returnsDigitsWithRequestedLength() {
        int len = 12;
        String number = AccountNumberGenerator.generateNumeric(len);
        assertEquals(len, number.length());
        assertTrue(number.matches("\\d{" + len + "}"));
    }
}
