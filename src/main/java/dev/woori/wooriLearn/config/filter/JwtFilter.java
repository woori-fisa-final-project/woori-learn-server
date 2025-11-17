package dev.woori.wooriLearn.config.filter;

import dev.woori.wooriLearn.config.jwt.JwtInfo;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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

    @Autowired
    private Environment environment;

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

    // JwtFilter에 Environment 주입 후
    private boolean isDevelopmentMode() {
        // 이 방법을 사용하려면 클래스에 private final Environment environment; 필드와 생성자 주입이 필요합니다.
        return environment.acceptsProfiles(Profiles.of("dev", "test"));
    }
}
