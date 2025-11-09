package dev.woori.wooriLearn.domain.user.entity;

import dev.woori.wooriLearn.config.BaseEntity;
import dev.woori.wooriLearn.config.exception.InvalidParameterException;
import dev.woori.wooriLearn.config.exception.InvalidStateException;
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

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;


    @Version
    @Column(nullable = false)
    private Integer version = 0;



    public void addPoints(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new InvalidParameterException("포인트 증액은 양수여야 합니다.");
        }
        this.points += amount;
    }

    public void subtractPoints(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new InvalidParameterException("포인트 차감은 양수여야 합니다.");
        }
        if (this.points < amount) {
            throw new InvalidStateException("포인트가 부족합니다.");
        }
        this.points -= amount;
    }

    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private boolean enabled = true;

    /** ✅ develop 쪽 추가된 Role 필드 **/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
