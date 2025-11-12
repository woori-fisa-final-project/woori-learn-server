package dev.woori.wooriLearn.domain.user.entity;

import dev.woori.wooriLearn.config.BaseEntity;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "auth_user_id", nullable = false)
    private AuthUsers authUser;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Integer points;

}
