package dev.woori.wooriLearn.jwtTest;

import dev.woori.wooriLearn.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class JwtUtilTest {
    private JwtIssuer jwtIssuer;
    private JwtValidator jwtValidator;

    // 테스트용 키
    private final String secretKey = Base64.getEncoder().encodeToString("this-is-a-test-secret-key-1234567890".getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        jwtIssuer = new JwtIssuer(secretKey);
        jwtValidator = new JwtValidator(secretKey);
        ReflectionTestUtils.setField(jwtIssuer, "accessTokenExpiration", 3600000);
        ReflectionTestUtils.setField(jwtIssuer, "refreshTokenExpiration", 604800000);
    }

    @Test
    @DisplayName("Access Token이 정상적으로 생성되고 username을 복호화할 수 있다")
    void generateAccessTokenAndExtractUsername() {
        // given
        String username = "testUser";

        // when
        String token = jwtIssuer.generateAccessToken(username, Role.ROLE_USER);

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtValidator.validateToken(token)).isTrue();
        assertThat(jwtValidator.getUsername(token)).isEqualTo(username);
    }

    @Test
    @DisplayName("Refresh Token도 정상적으로 생성되고 만료 시간이 올바르게 설정된다")
    void generateRefreshToken() {
        // given
        String username = "testUser";

        // when
        var tokenInfo = jwtIssuer.generateRefreshToken(username, Role.ROLE_USER);

        // then
        assertThat(tokenInfo.token()).isNotBlank();
        assertThat(tokenInfo.expiration()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("잘못된 서명 키로 서명된 토큰은 유효하지 않다")
    void invalidSignatureTokenShouldFailValidation() {
        // given
        String validToken = jwtIssuer.generateAccessToken("user1", Role.ROLE_USER);

        // 다른 키로 새 util 생성
        String otherKey = Base64.getEncoder().encodeToString("different-secret-key-1234567890".getBytes(StandardCharsets.UTF_8));
        JwtValidator invalidJwtValidator = new JwtValidator(otherKey);

        // when
        boolean isValid = invalidJwtValidator.validateToken(validToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 validateToken에서 false를 반환해야 한다")
    void expiredTokenShouldFailValidation() {
        // given
        long expiredMillis = -1000L; // 이미 만료된 시간
        var tokenInfo = jwtIssuer.generateToken("user1", Role.ROLE_USER, expiredMillis);

        // when
        boolean isValid = jwtValidator.validateToken(tokenInfo.token());

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰을 파싱하면 ExpiredJwtException이 발생한다")
    void getUsername_ShouldThrowExpiredJwtException_WhenTokenExpired() {
        // given
        var tokenInfo = jwtIssuer.generateToken("expiredUser", Role.ROLE_USER, -1000L);

        // then
        assertThatThrownBy(() -> jwtValidator.getUsername(tokenInfo.token()))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 validateToken에서 false를 반환해야 한다")
    void malformedTokenShouldFailValidation() {
        // given
        String malformedToken = "not.a.valid.token";

        // when
        boolean isValid = jwtValidator.validateToken(malformedToken);

        // then
        assertThat(isValid).isFalse();
    }
}
