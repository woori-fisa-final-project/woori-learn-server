package dev.woori.wooriLearn.config.ratelimit;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitInterceptor implements HandlerInterceptor {

    private final LettuceBasedProxyManager<String> proxyManager;

    @Value("${app.rate-limit.capacity:60}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:60}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration-minutes:1}")
    private int refillDurationMinutes;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * BucketConfiguration을 필드에 캐싱하여 매번 생성하는 비용 절감
     */
    private BucketConfiguration bucketConfiguration;

    /**
     * 인터셉터 초기화 시 BucketConfiguration을 한 번만 생성
     */
    @PostConstruct
    public void initBucketConfiguration() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofMinutes(refillDurationMinutes))
        );
        this.bucketConfiguration = BucketConfiguration.builder()
                .addLimit(limit)
                .build();

        log.info("Rate limit BucketConfiguration initialized - capacity: {}, refill: {} tokens per {} minute(s)",
                capacity, refillTokens, refillDurationMinutes);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String key;

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            key = authentication.getName(); // 인증된 사용자는 사용자 ID를 키로 사용
        } else {
            key = getClientIp(request); // 인증되지 않은 사용자는 IP를 키로 사용 (폴백)
        }
        String bucketKey = RATE_LIMIT_KEY_PREFIX + key;

        Bucket bucket = proxyManager.builder().build(bucketKey, () -> bucketConfiguration);

        if (bucket.tryConsume(1)) {
            long availableTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(availableTokens));
            log.debug("Rate limit OK - Key: {}, Remaining: {}", key, availableTokens);
            return true;
        } else {
            log.warn("Rate limit exceeded - Key: {}", key);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(refillDurationMinutes * 60));
            throw new CommonException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    String.format("요청 한도를 초과했습니다. %d분 후 다시 시도해주세요.", refillDurationMinutes)
            );
        }
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
            if (StringUtils.hasText(ipAddress) && !"unknown".equalsIgnoreCase(ipAddress)) {
                if (ipAddress.contains(",")) {
                    return ipAddress.split(",")[0].trim();
                }
                return ipAddress;
            }
        }
        return request.getRemoteAddr();
    }
}
