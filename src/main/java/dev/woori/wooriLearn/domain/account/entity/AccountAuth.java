package dev.woori.wooriLearn.domain.account.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name="account_auth",
        uniqueConstraints=@UniqueConstraint(columnNames="user_id")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    // 인증번호
    @Column(name = "auth_code", length = 6, nullable = false)
    private String authCode;
}
