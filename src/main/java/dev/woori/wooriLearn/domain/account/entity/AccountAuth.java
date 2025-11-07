package dev.woori.wooriLearn.domain.account.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name="account_auth",
        uniqueConstraints=@UniqueConstraint(columnNames="user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountAuth {

    public static final String AUTH_CODE_REGEX = "^\\d{6}$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 64, nullable = false, updatable = false)
    private String userId;

    // 인증번호
    @Column(name = "auth_code", length = 6, nullable = false)
    private String authCode;

    /** 인증번호 업데이트(의도된 경로로만 상태 변경) */
    public void updateAuthCode(String newCode) {
        if (newCode == null || !newCode.matches(AUTH_CODE_REGEX)) {
            throw new IllegalArgumentException("authCode는 6자리 숫자여야 합니다.");
        }
        this.authCode = newCode;
    }
}
