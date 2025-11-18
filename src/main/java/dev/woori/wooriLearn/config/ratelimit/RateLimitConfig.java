package dev.woori.wooriLearn.config.ratelimit;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rate Limiting 설정
 * - 자동이체 API에 Rate Limit 인터셉터 적용
 * - Redis 관련 Bean들을 Spring 컨테이너가 생명주기 관리
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/education/auto-payment/**")
                .order(1); // 첫 번째로 실행
    }

    /**
     * Redis 클라이언트 빈 등록
     * @param redisHost Redis 호스트
     * @param redisPort Redis 포트
     * @return RedisClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort) {

        log.info("Creating RedisClient for Rate Limiting - Redis: {}:{}", redisHost, redisPort);
        return RedisClient.create("redis://" + redisHost + ":" + redisPort);
    }

    /**
     * Redis 연결 빈 등록
     * @param redisClient Redis 클라이언트
     * @return StatefulRedisConnection 인스턴스
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        log.info("Creating Redis connection for Rate Limiting");
        return redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
    }

    /**
     * Bucket4j Redis 기반 ProxyManager 빈 등록
     * @param connection Redis 연결
     * @return LettuceBasedProxyManager 인스턴스
     */
    @Bean
    public LettuceBasedProxyManager<String> lettuceBasedProxyManager(
            StatefulRedisConnection<String, byte[]> connection) {

        log.info("Initializing LettuceBasedProxyManager for Rate Limiting");
        return LettuceBasedProxyManager.builderFor(connection).build();
    }
}
