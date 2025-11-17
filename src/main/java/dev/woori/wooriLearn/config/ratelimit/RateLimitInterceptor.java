package dev.woori.wooriLearn.config.ratelimit;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * API 호출 Rate Limiting 인터셉터
 * - IP 기반으로 요청 제한
 * - Redis 기반 Token Bucket 알고리즘 사용 (분산 환경 지원)
 * - 자동이체 API에만 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.rate-limit.capacity:60}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:60}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration-minutes:1}")
    private int refillDurationMinutes;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String TOKENS_SUFFIX = ":tokens";
    private static final String LAST_REFILL_SUFFIX = ":last_refill";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Rate Limiting 비활성화 시 통과
        if (!enabled) {
            return true;
        }

        String ip = getClientIp(request);
        String tokensKey = RATE_LIMIT_KEY_PREFIX + ip + TOKENS_SUFFIX;
        String lastRefillKey = RATE_LIMIT_KEY_PREFIX + ip + LAST_REFILL_SUFFIX;

        // 현재 시간
        long now = System.currentTimeMillis();

        // Redis에서 현재 토큰 수와 마지막 리필 시간 조회
        String tokensStr = redisTemplate.opsForValue().get(tokensKey);
        String lastRefillStr = redisTemplate.opsForValue().get(lastRefillKey);

        int currentTokens;
        long lastRefillTime;

        if (tokensStr == null || lastRefillStr == null) {
            // 첫 요청: 초기화
            currentTokens = capacity;
            lastRefillTime = now;
        } else {
            currentTokens = Integer.parseInt(tokensStr);
            lastRefillTime = Long.parseLong(lastRefillStr);

            // 경과 시간에 따른 토큰 리필
            long elapsedMinutes = (now - lastRefillTime) / (60 * 1000);
            if (elapsedMinutes > 0) {
                int tokensToAdd = (int) (elapsedMinutes * refillTokens / refillDurationMinutes);
                currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
                lastRefillTime = now;
            }
        }

        if (currentTokens > 0) {
            // 요청 허용: 토큰 1개 소비
            currentTokens--;

            // Redis 업데이트
            redisTemplate.opsForValue().set(tokensKey, String.valueOf(currentTokens),
                    Duration.ofMinutes(refillDurationMinutes * 2));
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(lastRefillTime),
                    Duration.ofMinutes(refillDurationMinutes * 2));

            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(currentTokens));
            log.debug("Rate limit OK - IP: {}, Remaining: {}", ip, currentTokens);
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
