package dev.woori.wooriLearn.config.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Base64;

@Component
public class Sha256Encoder implements Encoder{

    @Override
    public String encode(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode token", e);
        }
    }

    @Override
    public boolean matches(String rawValue, String encodedValue) {
        return encode(rawValue).equals(encodedValue);
    }
}
