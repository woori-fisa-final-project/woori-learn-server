package dev.woori.wooriLearn.config.filter;

import dev.woori.wooriLearn.config.jwt.JwtInfo;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;
    private final String BEARER = "Bearer ";

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 헤더에서 authorization 토큰 가져오기
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (accessToken != null && accessToken.startsWith(BEARER)) {
            String token = accessToken.substring(BEARER.length()); // 순수 토큰값만 가져오기
            JwtInfo jwtInfo = jwtValidator.parseToken(token);

            // Authentication 객체 생성
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(jwtInfo.username(), null,
                            List.of(new SimpleGrantedAuthority(jwtInfo.role().name())));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (isDevelopmentMode()) {
            // dev/test 프로파일에서 토큰이 없는 경우, 기본 테스트 사용자로 인증 처리
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken("testuser", null,
                            List.of(new SimpleGrantedAuthority(Role.ROLE_USER.name())));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 개발 환경인지 확인
     * @return dev 또는 test 프로파일인 경우 true
     */
    private boolean isDevelopmentMode() {
        return "dev".equals(activeProfile) || "test".equals(activeProfile);
    }
}
