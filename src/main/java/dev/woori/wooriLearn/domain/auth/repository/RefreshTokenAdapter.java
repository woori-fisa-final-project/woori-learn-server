package dev.woori.wooriLearn.domain.auth.repository;

import dev.woori.wooriLearn.domain.auth.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Optional<RefreshToken> findByUsername(String username) {
        return refreshTokenRepository.findByUsername(username);
    }

    @Override
    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return refreshTokenRepository.save(token);
    }
}
