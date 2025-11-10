package dev.woori.wooriLearn.domain.common.auth;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;

public final class PrincipalUtils {

    private PrincipalUtils() {}

    public static String extractUsername(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof java.security.Principal p) {
            return p.getName();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }
        return null;
    }

    public static String requireUsername(Object principal) {
        String username = extractUsername(principal);
        if (username == null || username.isBlank()) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return username;
    }
}

