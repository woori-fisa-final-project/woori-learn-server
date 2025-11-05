package dev.woori.wooriLearn.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;

    @Value("${spring.jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public JwtUtil(@Value("${spring.jwt.secret}") String secret) {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            System.out.println("토큰 만료 시각: " + claims.getExpiration());
            System.out.println("현재 시각: " + new Date());
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("만료된 토큰입니다: " + e.getClaims().getExpiration());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("JWT 예외: " + e.getMessage());
            return false;
        }
    }

    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .claim("username", username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .claim("username", username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    public LocalDateTime getRefreshTokenExpiration() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul")).plus(refreshTokenExpiration, ChronoUnit.MILLIS);
    }

}
