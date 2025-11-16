package dev.woori.wooriLearn.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 비밀번호 해시 생성 유틸리티
 *
 * Usage: ./gradlew generateBCrypt
 */
public class BCryptGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String[] passwords = args.length > 0 ? args : new String[]{"1234"};

        System.out.println("=".repeat(80));
        System.out.println("BCrypt Password Hash Generator");
        System.out.println("=".repeat(80));

        for (String password : passwords) {
            String encoded = encoder.encode(password);
            boolean matches = encoder.matches(password, encoded);

            System.out.println("\nPlain Password: " + password);
            System.out.println("BCrypt Hash:    " + encoded);
            System.out.println("Verification:   " + (matches ? "✓ PASS" : "✗ FAIL"));
            System.out.println("-".repeat(80));
        }

        System.out.println("\nSQL UPDATE 예시:");
        System.out.println("UPDATE educational_account SET account_password = '" +
                          encoder.encode("1234") + "' WHERE id = 1;");
        System.out.println("=".repeat(80));
    }
}
