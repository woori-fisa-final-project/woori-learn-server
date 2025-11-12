package dev.woori.wooriLearn.config.jwt;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtValidator {

    private final SecretKey secretKey;

    public JwtValidator(@Value("${spring.jwt.secret}") String secret) {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtInfo parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            Role role = Role.valueOf(claims.get("role", String.class));

            return new JwtInfo(username, role);
        } catch (ExpiredJwtException e) {
            throw new CredentialsExpiredException("토큰이 만료되었습니다.", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("유효하지 않은 토큰입니다.", e);
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Role getRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST);
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("[JwtUtil] Token expired: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new CommonException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtUtil] Token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new CommonException(ErrorCode.INVALID_REQUEST);
        }
    }
}
