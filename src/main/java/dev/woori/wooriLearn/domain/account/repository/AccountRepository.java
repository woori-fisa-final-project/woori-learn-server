package dev.woori.wooriLearn.domain.account.repository;


import dev.woori.wooriLearn.domain.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 유저가 가진 계좌 목록
    List<Account> findAllByUser_Id(Long userId);

    // 계좌번호로 조회 (환전 신청에서 사용)
    Optional<Account> findByAccountNumber(String accountNumber);
}