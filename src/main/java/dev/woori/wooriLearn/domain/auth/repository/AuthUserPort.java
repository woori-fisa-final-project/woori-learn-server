package dev.woori.wooriLearn.domain.auth.repository;

import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;

import java.util.Optional;

public interface AuthUserPort {
    Optional<AuthUsers> findByUserId(String userId);

    Boolean existsByUserId(String userId);

    AuthUsers save(AuthUsers authUser);
}
