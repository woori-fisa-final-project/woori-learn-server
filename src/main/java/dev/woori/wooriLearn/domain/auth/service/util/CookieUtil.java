package dev.woori.wooriLearn.domain.auth.service.util;

import lombok.Getter;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CookieUtil {

    public static ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public static ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(0)
                .build();
    }
}
