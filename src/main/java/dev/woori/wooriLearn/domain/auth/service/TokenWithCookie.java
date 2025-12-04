package dev.woori.wooriLearn.domain.auth.service;

import org.springframework.http.ResponseCookie;

public record TokenWithCookie(
        String accessToken,
        String role,
        ResponseCookie cookie
) {
}
