package dev.woori.wooriLearn.config.ratelimit;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
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
     * - RedisProperties를 주입받아 application.properties의 모든 Redis 설정 반영
     * - 비밀번호, 데이터베이스, SSL 등 추가 설정 지원
     * @param redisProperties Spring Boot Redis 설정
     * @return RedisClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(RedisProperties redisProperties) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withDatabase(redisProperties.getDatabase());

        // 비밀번호 설정 (있는 경우)
        if (StringUtils.hasText(redisProperties.getPassword())) {
            builder.withPassword(redisProperties.getPassword().toCharArray());
        }

        // SSL 설정 (활성화된 경우)
        if (redisProperties.getSsl().isEnabled()) {
            builder.withSsl(true);
        }

        RedisURI redisURI = builder.build();

        log.info("Creating RedisClient for Rate Limiting - Redis: {}:{}, DB: {}, SSL: {}",
                redisProperties.getHost(),
                redisProperties.getPort(),
                redisProperties.getDatabase(),
                redisProperties.getSsl().isEnabled());

        return RedisClient.create(redisURI);
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
