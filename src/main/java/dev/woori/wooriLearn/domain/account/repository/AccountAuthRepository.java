package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.AccountAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountAuthRepository extends JpaRepository<AccountAuth, Long> {
    Optional<AccountAuth> findByUserId(String userId);
    void deleteByUserId(String userId);
}
