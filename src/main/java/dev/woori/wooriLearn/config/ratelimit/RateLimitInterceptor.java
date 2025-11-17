package dev.woori.wooriLearn.config.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * API 호출 Rate Limiting 인터셉터
 * - IP 기반으로 요청 제한
 * - Bucket4j + Caffeine Cache 사용
 * - 자동이체 API에만 적용
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${app.rate-limit.capacity:60}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:60}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration-minutes:1}")
    private int refillDurationMinutes;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Rate Limiting 비활성화 시 통과
        if (!enabled) {
            return true;
        }

        String ip = getClientIp(request);
        Bucket bucket = resolveBucket(ip);

        if (bucket.tryConsume(1)) {
            // 요청 허용
            long availableTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(availableTokens));
            log.debug("Rate limit OK - IP: {}, Remaining: {}", ip, availableTokens);
            return true;
        } else {
            // 요청 제한 초과
            log.warn("Rate limit exceeded - IP: {}", ip);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(refillDurationMinutes * 60));
            throw new CommonException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    String.format("요청 한도를 초과했습니다. %d분 후 다시 시도해주세요.", refillDurationMinutes)
            );
        }
    }

    /**
     * IP별 Bucket 생성 또는 조회
     */
    private Bucket resolveBucket(String ip) {
        return cache.get(ip, key -> createNewBucket());
    }

    /**
     * 새로운 Bucket 생성
     * - 용량: capacity (기본 60)
     * - 리필: refillTokens개/refillDurationMinutes분 (기본 60개/1분)
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofMinutes(refillDurationMinutes))
        );
        return Bucket.builder()
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

    /**
     * 클라이언트 IP 추출
     * - 프록시 환경 고려 (X-Forwarded-For 헤더 우선)
     */
    private String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ipAddress = request.getHeader(header);
            if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
                // X-Forwarded-For 헤더는 여러 IP를 포함할 수 있으므로 첫 번째 IP를 사용합니다.
                if (ipAddress.contains(",")) {
                    return ipAddress.split(",")[0].trim();
                }
                return ipAddress;
            }
        }

        return request.getRemoteAddr();
    }
}
