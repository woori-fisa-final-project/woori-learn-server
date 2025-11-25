package dev.woori.wooriLearn.util;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

/**
 * 인증 관련 유틸리티 클래스
 * - Authentication 객체에서 사용자 ID 추출
 */
@Slf4j
public class AuthenticationUtil {

    private AuthenticationUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Authentication 객체에서 사용자 ID 추출
     * @param authentication 인증 객체
     * @return 사용자 ID
     * @throws CommonException 인증 정보가 없는 경우
     */
    public static String extractUserId(Authentication authentication) {
        if (authentication == null) {
            log.error("인증 정보가 없는 요청 감지");
            throw new CommonException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return authentication.getName();
    }
}
