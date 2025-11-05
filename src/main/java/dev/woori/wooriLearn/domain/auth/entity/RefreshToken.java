package dev.woori.wooriLearn.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(length = 500)
    private String token;

    private LocalDateTime expiration;

    public void updateToken(String newToken, LocalDateTime newExpiration) {
        this.token = newToken;
        this.expiration = newExpiration;
    }
}
