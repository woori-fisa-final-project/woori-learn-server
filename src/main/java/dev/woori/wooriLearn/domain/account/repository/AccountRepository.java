package dev.woori.wooriLearn.domain.account.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AccountRepository extends JpaRepository<EducationalAccount, Long> {

    /*
        특정 사용자 Id로 계좌 목록 조회

        educational_account 테이블에 접근하여 user_id로 계좌 목록 조회
        JPA를 통해 자동으로 SQL 쿼리 생성함
        @parm userId : 조회된 사용자 ID
        @return List<EducationalAccount> : 해당 사용자가 소유한 모든 계좌 리스트
     */

    // findBy + [엔티티 필드명] => JPA가 user_Id 컬럼을 조건으로 자동 쿼리 생성 됨.
    List<EducationalAccount> findByUserId(Long userId);

}
