package dev.woori.wooriLearn.jwtTest;

import dev.woori.wooriLearn.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

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
        jwtIssuer = new JwtIssuer(secretKey, 3600000L, 604800000L);
        jwtValidator = new JwtValidator(secretKey);
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
        assertThat(jwtValidator.parseToken(token).username()).isEqualTo(username);
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
    @DisplayName("만료된 토큰을 파싱하면 CredentialsExpiredException이 발생한다")
    void getUsername_ShouldThrowExpiredJwtException_WhenTokenExpired() {
        // given
        var tokenInfo = jwtIssuer.generateToken("expiredUser", Role.ROLE_USER, -1000L);

        // then
        assertThatThrownBy(() -> jwtValidator.parseToken(tokenInfo.token()))
                .isInstanceOf(CredentialsExpiredException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰을 파싱하면 BadCredentialsException이 발생한다")
    void malformedTokenShouldFailValidation() {
        // given
        String malformedToken = "not.a.valid.token";

        // when
        assertThatThrownBy(() -> jwtValidator.parseToken(malformedToken))
                .isInstanceOf(BadCredentialsException.class);
    }
}
