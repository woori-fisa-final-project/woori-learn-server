package dev.woori.wooriLearn.util;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;

/**
 * 인증 관련 유틸리티 클래스
 * - 다양한 형태의 인증 객체에서 사용자 ID 추출
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

    /**
     * @AuthenticationPrincipal로 받은 principal 객체에서 사용자 ID 추출
     * @param principal 인증 주체 객체
     * @return 사용자 ID
     * @throws CommonException 인증 정보가 없거나 유효하지 않은 경우
     */
    public static String extractUserId(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof Principal p) {
            return p.getName();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }

        log.error("유효하지 않은 principal 타입: {}", principal != null ? principal.getClass() : "null");
        throw new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
