package dev.woori.wooriLearn.domain.user.service.util;

import java.security.SecureRandom;

public class AccountNumberGenerator {
    private static final SecureRandom random = new SecureRandom();

    // 원하는 길이만큼 숫자 문자열 생성
    public static String generateNumeric(int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 0~9
        }

        return sb.toString();
    }
}
