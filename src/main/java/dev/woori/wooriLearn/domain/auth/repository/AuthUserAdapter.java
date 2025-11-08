package dev.woori.wooriLearn.domain.auth.repository;

import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthUserAdapter implements AuthUserPort{

    private final AuthUserRepository authUserRepository;

    @Override
    public Optional<AuthUsers> findByUserId(String userId) {
        return authUserRepository.findByUserId(userId);
    }

    @Override
    public Boolean existsByUserId(String userId) {
        return authUserRepository.existsByUserId(userId);
    }

    @Override
    public AuthUsers save(AuthUsers authUser) {
        return authUserRepository.save(authUser);
    }
}
