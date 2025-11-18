package dev.woori.wooriLearn.domain.auth.service.util;

import org.springframework.http.ResponseCookie;

public final class CookieUtil {

    // 인스턴스화 방지용 private 생성자
    private CookieUtil() {
    }

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
