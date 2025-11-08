package dev.woori.wooriLearn.domain.auth.repository;

import dev.woori.wooriLearn.domain.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenPort {
    Optional<RefreshToken> findByUsername(String username);

    void deleteByUsername(String username);

    RefreshToken save(RefreshToken token);
}
