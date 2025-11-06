package dev.woori.wooriLearn.config.jwt;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static java.time.Instant.now;

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
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateAccessToken(String username) {
        return generateToken(username, accessTokenExpiration).token();
    }

    public TokenInfo generateRefreshToken(String username) {
        return generateToken(username, refreshTokenExpiration);
    }

    public TokenInfo generateToken(String username, long expirationMilis){
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMilis);

        String token = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .claim("username", username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();

        return new TokenInfo(token, expiration);
    }

}
