package dev.woori.wooriLearn.domain.edubankapi.entity;

import dev.woori.wooriLearn.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 *  [Entity] 교육용 계좌 테이블
 *
 *  시나리오용 계좌 데이터를 관리하는 엔티티
 *  - 한 명의 사용자는 여러 개의 교육용 계좌를 가질 수 있음. (1:N)
 *  - 한 개의 계좌에는 여러 거래내역이 존재함(1:N)
 */

@Entity
@Getter
@Table(name = "educational_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EducationalAccount {

    // 기본키 자동 증가
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 계좌번호
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    // 현재 잔액
    @Column(nullable = false)
    private Integer balance;

    // 계좌 비밀번호
    @Column(name = "account_password", nullable = false, length = 4)
    private String accountPassword;

    // 계좌명
    @Column(name = "account_name", nullable = false, length = 30)
    private String accountName;

    /*
        사용자 엔티티와 연관관계
        - 한명의 사용자가 여러 개의 계좌를 소유할 수 있음 1:N
        - EducationalAccount는 N 쪽으로 user_id 외래키를 가짐
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /*
        거래내역와 엔티티와 연관관계
        - 한 계좌에는 여러 거래내역이 존재할 수 있음 1:N
     */
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<TransactionHistory> transactionHistories;
}
