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

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Integer points;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    /* 포인트 전체값 변경 */
    public void setPoints(Integer points) {
        this.points = points;
    }

    /* 포인트 증가 */
    public void addPoints(Integer amount) {
        this.points += amount;
    }
    /* 포인트 차감 */
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

}
