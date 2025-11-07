package dev.woori.wooriLearn.config.security;

import dev.woori.wooriLearn.config.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // ← @PreAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    // 인증 없이 접근 가능한 엔드포인트(운영용 화이트리스트)
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/signup"
    );

    // =========================
    // 개발 프로필: Basic Auth
    // =========================
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 개발에선 /account/** 는 인증 필요, 나머지는 허용
                        .requestMatchers("/account/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults()) // ← Postman Basic Auth로 테스트
                .build();
    }

    // dev 프로필에서 사용할 인메모리 사용자 (Postman에서 인증 주체 생성)
    @Bean
    @Profile("dev")
    public UserDetailsService inMemoryUsers(PasswordEncoder encoder) {
        // 기본 사용자: U123 / pass123 (원하면 yml로 꺼내서 주입해도 됩니다)
        UserDetails user = User.withUsername("U123")
                .password(encoder.encode("pass123"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    // =========================
    // 운영/기타 프로필: JWT
    // =========================
    @Bean
    @Profile("!dev")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(WHITE_LIST.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated() // 나머지는 JWT 필요
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 공용 PasswordEncoder (dev/prod 공통)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}