package dev.woori.wooriLearn.config.security;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public Long requireUserId(Object principal) {
        String username = extractUsername(principal);
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
        return user.getId();
    }

    public String extractUsername(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) return ud.getUsername();
        if (principal instanceof java.security.Principal p) return p.getName();
        if (principal instanceof String s && !"anonymousUser".equals(s)) return s;
        throw new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}

