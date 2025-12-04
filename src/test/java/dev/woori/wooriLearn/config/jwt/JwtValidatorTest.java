package dev.woori.wooriLearn.config.jwt;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtValidatorTest {

    private static final String SECRET = "01234567890123456789012345678901"; // 32 bytes

    private JwtValidator validator() {
        return new JwtValidator(SECRET);
    }

    @Test
    @DisplayName("유효한 토큰을 JwtInfo로 파싱한다")
    void parseToken_success() {
        String token = Jwts.builder()
                .setSubject("user1")
                .claim("role", "ROLE_USER")
                .setExpiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        JwtInfo info = validator().parseToken(token);
        assertEquals("user1", info.username());
        assertEquals(Role.ROLE_USER, info.role());
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 예외를 던진다")
    void parseToken_expiredThrows() {
        String token = Jwts.builder()
                .setSubject("user1")
                .claim("role", "ROLE_USER")
                .setExpiration(Date.from(Instant.now().minusSeconds(10)))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        CommonException ex = assertThrows(CommonException.class, () -> validator().parseToken(token));
        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("손상된 토큰이면 TOKEN_UNAUTHORIZED 예외를 던진다")
    void parseToken_invalidThrowsUnauthorized() {
        CommonException ex = assertThrows(CommonException.class, () -> validator().parseToken("invalid.token.here"));
        assertEquals(ErrorCode.TOKEN_UNAUTHORIZED, ex.getErrorCode());
    }
}
