package dev.woori.wooriLearn.config.ratelimit;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * API 호출 Rate Limiting 인터셉터
 * - IP 기반으로 요청 제한
 * - Bucket4j + Redis 사용 (원자적 연산 보장, 분산 환경 지원)
 * - 자동이체 API에만 적용
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitInterceptor implements HandlerInterceptor {

    private final LettuceBasedProxyManager<String> proxyManager;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, byte[]> connection;

    @Value("${app.rate-limit.capacity:60}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:60}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration-minutes:1}")
    private int refillDurationMinutes;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    public RateLimitInterceptor(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort) {

        this.redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        this.connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();
    }

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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = getClientIp(request);
        String bucketKey = RATE_LIMIT_KEY_PREFIX + ip;

        Bucket bucket = proxyManager.builder().build(bucketKey, getConfigurationSupplier());

        if (bucket.tryConsume(1)) {
            long availableTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(availableTokens));
            log.debug("Rate limit OK - IP: {}, Remaining: {}", ip, availableTokens);
            return true;
        } else {
            log.warn("Rate limit exceeded - IP: {}", ip);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(refillDurationMinutes * 60));
            throw new CommonException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    String.format("요청 한도를 초과했습니다. %d분 후 다시 시도해주세요.", refillDurationMinutes)
            );
        }
    }

    private BucketConfiguration getConfigurationSupplier() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofMinutes(refillDurationMinutes))
        );
        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED",
            "X-Real-IP"
    };

    private String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ipAddress = request.getHeader(header);
            if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
                if (ipAddress.contains(",")) {
                    return ipAddress.split(",")[0].trim();
                }
                return ipAddress;
            }
        }
        return request.getRemoteAddr();
    }
}
