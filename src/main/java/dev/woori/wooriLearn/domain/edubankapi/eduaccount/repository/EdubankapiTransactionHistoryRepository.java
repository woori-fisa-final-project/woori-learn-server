package dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository;


import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

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

    // JPA가 메서드 이름을 해석해서 자동으로 쿼리를 만드는데
    // 그 네이밍 규칙에 맞춰서 변수명을 쓰니까 이렇게 길어졌습니다.
    List<TransactionHistory> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc
    (
            Long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

}
