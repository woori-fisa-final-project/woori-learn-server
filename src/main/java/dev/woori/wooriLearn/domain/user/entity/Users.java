package dev.woori.wooriLearn.domain.user.entity;

import dev.woori.wooriLearn.config.BaseEntity;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id", nullable = false)
    private AuthUsers authUser;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    public void addPoints(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "포인트 증액은 양수여야 합니다.");
        }
        this.points += amount;
    }

    public void subtractPoints(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "포인트 차감은 양수여야 합니다.");
        }
        if (this.points < amount) {
            throw new CommonException(ErrorCode.CONFLICT, "포인트가 부족합니다.");
        }
        this.points -= amount;
    }
}
