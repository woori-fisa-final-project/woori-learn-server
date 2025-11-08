package dev.woori.wooriLearn.domain.user.entity;

import dev.woori.wooriLearn.config.BaseEntity;
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
    private Integer points;

    /** ✅ feat/#21 에서 추가된 부분 **/
    @Version
    @Column(nullable = false)
    private Integer version = 0;

    public void setPoints(Integer points) {
        this.points = points;
    }

    public void addPoints(Integer amount) {
        this.points += amount;
    }

    public void subtractPoints(Integer amount) {
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
