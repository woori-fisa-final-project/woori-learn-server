package dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 계좌이체, 잔액 차감, 입금 처리도 EdubankapiAccountRepository에서 진행
 */
public interface EdubankapiAccountRepository extends JpaRepository<EducationalAccount, Long> {

    /**
     * 특정 사용자 Id로 계좌 목록 조회
     *
     * educational_account 테이블에 접근하여 user_id로 계좌 목록 조회
     * JPA를 통해 자동으로 SQL 쿼리 생성함
     *
     * @param userId : 조회된 사용자 ID (PK)
     * @return List<EducationalAccount> : 해당 사용자가 소유한 모든 계좌 리스트
     */
    @EntityGraph(attributePaths = {"user"})
    List<EducationalAccount> findByUser_Id(Long userId);

    /**
     * 계좌번호 존재 여부 확인
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * [이체/수정용] 계좌번호로 계좌 조회 (비관적 락 O)
     *
     * - 잔액 차감/증가 등 '동시성 충돌 위험'이 있는 수정 트랜잭션에서만 사용
     * - 동시에 같은 계좌를 수정하는 것을 방지
     *
     * @param accountNumber 계좌번호
     * @return Optional<EducationalAccount> (User 정보 포함)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EducationalAccount e JOIN FETCH e.user WHERE e.accountNumber = :accountNumber")
    Optional<EducationalAccount> findByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * [단순 조회용] 계좌번호로 계좌 조회 (비관적 락 X)
     *
     * - 비밀번호 확인, 단순 잔액 조회 등 데이터 수정이 없는 작업에 사용
     * - 락을 걸지 않아 동시성 성능 저하를 방지함
     * - N+1 방지를 위해 JOIN FETCH e.user는 유지
     *
     * @param accountNumber 계좌번호
     * @return Optional<EducationalAccount> (User 정보 포함)
     */
    @Query("SELECT e FROM EducationalAccount e JOIN FETCH e.user WHERE e.accountNumber = :accountNumber")
    Optional<EducationalAccount> findByAccountNumberForRead(@Param("accountNumber") String accountNumber);

    /**
     * ID로 계좌 조회 (User 정보 JOIN FETCH - N+1 문제 방지)
     *
     * validateAccountOwnership에서 account.getUser().getUserId() 호출 시
     * LAZY 로딩으로 인한 추가 쿼리 발생을 방지하기 위해 JOIN FETCH 사용
     *
     * @param id 계좌 ID
     * @return Optional<EducationalAccount> (User 정보 포함)
     */
    @Query("SELECT e FROM EducationalAccount e JOIN FETCH e.user WHERE e.id = :id")
    Optional<EducationalAccount> findByIdWithUser(@Param("id") Long id);

    /**
     * 계좌 소유권 존재 여부 확인 (효율적인 쿼리)
     *
     * 목록 조회 API에서 소유권만 확인할 때 사용
     * SELECT COUNT(*) 쿼리로 엔티티를 로드하지 않고 존재 여부만 확인
     *
     * @param id 계좌 ID
     * @param userId 사용자 ID (String login ID)
     * @return 계좌가 존재하고 해당 사용자 소유인지 여부
     */
    boolean existsByIdAndUser_UserId(Long id, String userId);

}