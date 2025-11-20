package dev.woori.wooriLearn;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    @Test
    public void generatePasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password1 = "test1234";
        String password2 = "1234";

        System.out.println("=================================");
        System.out.println("Password: test1234");
        System.out.println("Hash: " + encoder.encode(password1));
        System.out.println();
        System.out.println("Password: 1234");
        System.out.println("Hash: " + encoder.encode(password2));
        System.out.println("=================================");
    }
}
