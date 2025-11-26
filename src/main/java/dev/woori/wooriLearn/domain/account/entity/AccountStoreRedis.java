package dev.woori.wooriLearn.domain.account.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// redis 저장소
@Component
@RequiredArgsConstructor
public class AccountStoreRedis {
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${spring.data.redis.ttl}")
    private Duration sessionTtl;

    // 세션 id를 키값으로 session 객체를 저장
    public void save(String sessionId, AccountSession session) {
        redisTemplate.opsForValue().set(sessionId, session, sessionTtl);
    }

    // id에 매핑된 객체를 불러옴
    public AccountSession get(String sessionId) {
        Object obj = redisTemplate.opsForValue().get(sessionId);
        return (obj instanceof AccountSession session) ? session : null;
    }

    public void delete(String sessionId) {
        redisTemplate.delete(sessionId);
    }
}
