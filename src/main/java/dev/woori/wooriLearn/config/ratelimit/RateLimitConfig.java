package dev.woori.wooriLearn.config.ratelimit;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
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
 * - LettuceBasedProxyManager를 Spring Bean으로 관리하여 리소스 누수 방지
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    private StatefulRedisConnection<String, byte[]> connection;
    private RedisClient redisClient;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/education/auto-payment/**")
                .order(1); // 첫 번째로 실행
    }

    /**
     * Bucket4j Redis 기반 ProxyManager 빈 등록
     * @param redisHost Redis 호스트
     * @param redisPort Redis 포트
     * @return LettuceBasedProxyManager 인스턴스
     */
    @Bean
    public LettuceBasedProxyManager<String> lettuceBasedProxyManager(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort) {

        this.redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        this.connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        log.info("Initialized LettuceBasedProxyManager for Rate Limiting - Redis: {}:{}", redisHost, redisPort);

        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    /**
     * Redis 연결 리소스 정리
     */
    @PreDestroy
    public void destroy() {
        log.info("Closing Redis connection and client for Rate Limiting...");
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}
