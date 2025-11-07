package dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository;


import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EdubankapiTransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    /**
     *  특정 계좌번호에 해당하는 거래내역을 기간 기준으로 조회하기
     *  최신순으로 정렬 (desc)
     *
     *  @param accountId 계좌번호
     *  @param startDate 조회 시작일
     *  @param endDate 조회 종료일
     *  @return 거래내역 리스트
     */

    @Query("SELECT th FROM TransactionHistory th WHERE th.account.id = :accountId AND th.transactionDate BETWEEN :startDate AND :endDate ORDER BY th.transactionDate DESC")
    List<TransactionHistory> findTransactionsByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
