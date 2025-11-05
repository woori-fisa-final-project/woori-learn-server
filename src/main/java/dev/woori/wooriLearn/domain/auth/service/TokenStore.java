package dev.woori.wooriLearn.domain.auth.service;

public interface TokenStore {
    void saveRefreshToken(Long userId, String refreshToken, long expirationMillis);
    String getRefreshToken(Long userId);
    void deleteRefreshToken(Long userId);
}
