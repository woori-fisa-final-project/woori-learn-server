package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.account.entity.AccountAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountAuthRepository extends JpaRepository<AccountAuth, Long> {
    Optional<AccountAuth> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM AccountAuth a WHERE a.userId = :userId")
    void deleteByUserId(String userId);
}
