package dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


/**
 *      계좌이체, 잔액 차감, 입금 처리도 EdubankapiAccountRepository에서 진행
 */

public interface EdubankapiAccountRepository extends JpaRepository<EducationalAccount, Long> {

    /**
     *      특정 사용자 Id로 계좌 목록 조회
     *
     *      educational_account 테이블에 접근하여 user_id로 계좌 목록 조회
     *      JPA를 통해 자동으로 SQL 쿼리 생성함
     *      @param userId : 조회된 사용자 ID
     *      @return List<EducationalAccount> : 해당 사용자가 소유한 모든 계좌 리스트
     */

    // findBy + [엔티티 필드명] => JPA가 user_Id 컬럼을 조건으로 자동 쿼리 생성 됨.
    List<EducationalAccount> findByUserId(Long userId);

    /**
     * 계좌번호로 계좌 조회
     *
     * Optional로 감싼 이유
     * -> 계좌가 존재하지 않을 수도 있기 때문에 예외처리를 위해서
     *
     * @Lock (비관적 락 적용)
     * - 동시에 같은 계좌를 수정하는 것을 방지
     * -> 동시 이체 요청 시 잔액 불일치 / 중복 이체 방지
     * @Query
     * -> 명시적으로 JPQL 작성 ( 자동 메서드 네이밍보다 명확)
     * => JPA 메서드 네이밍 규칙 기반 쿼리가 내부적으로 락 옵셥을 명확히 적용하지 못하거나, 상황에 따라 DB 벤더별 쿼리가 달라질 수 있기 때문에 사용
     *
     * @param accountNumber 계좌번호
     * @return Optional<EducationalAccount>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EducationalAccount e WHERE e.accountNumber = :accountNumber")
    Optional<EducationalAccount> findByAccountNumber(@Param("accountNumber") String accountNumber);

}
