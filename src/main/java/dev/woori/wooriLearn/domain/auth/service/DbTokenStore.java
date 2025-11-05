package dev.woori.wooriLearn.domain.auth.service;

import dev.woori.wooriLearn.domain.auth.entity.RefreshToken;
import dev.woori.wooriLearn.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbTokenStore implements TokenStore {

    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public void saveRefreshToken(Long userId, String refreshToken, long expirationMillis) {
//        RefreshToken entity = RefreshToken.builder()
//                .username(userId)
//                .build();
//        refreshTokenRepository.save(entity);
    }

    @Override
    public String getRefreshToken(Long userId) {
        return "";
    }

    @Override
    public void deleteRefreshToken(Long userId) {

    }
}
