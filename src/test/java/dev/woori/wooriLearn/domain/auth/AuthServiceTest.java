package dev.woori.wooriLearn.domain.auth;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.jwt.JwtInfo;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.config.jwt.TokenInfo;
import dev.woori.wooriLearn.config.security.Encoder;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import dev.woori.wooriLearn.domain.auth.dto.LoginResDto;
import dev.woori.wooriLearn.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.RefreshToken;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    private AuthUserPort authUserPort;
    private RefreshTokenPort refreshTokenPort;
    private PasswordEncoder passwordEncoder;
    private Encoder encoder;
    private JwtIssuer jwtIssuer;
    private JwtValidator jwtValidator;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authUserPort = mock(AuthUserPort.class);
        refreshTokenPort = mock(RefreshTokenPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        encoder = mock(Encoder.class);
        jwtIssuer = mock(JwtIssuer.class);
        jwtValidator = mock(JwtValidator.class);

        authService = new AuthService(authUserPort, passwordEncoder, encoder, jwtIssuer, jwtValidator, refreshTokenPort);
    }

    // ------------------------------------------------------------------------
    // 로그인
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공 시 access/refresh 토큰이 생성되고 저장된다")
    void login_success() {
        // given
        LoginReqDto req = new LoginReqDto("user1", "rawPw");
        AuthUsers mockUser = AuthUsers.builder()
                .userId("user1")
                .password("encodedPw")
                .role(Role.ROLE_USER)
                .build();

        when(authUserPort.findByUserId("user1")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("rawPw", "encodedPw")).thenReturn(true);

        when(jwtIssuer.generateAccessToken("user1", Role.ROLE_USER))
                .thenReturn("access-token");
        when(jwtIssuer.generateRefreshToken("user1", Role.ROLE_USER))
                .thenReturn(new TokenInfo("refresh-token", Instant.now().plusSeconds(3600)));

        when(encoder.encode("refresh-token")).thenReturn("encoded-refresh-token");
        when(refreshTokenPort.findByUsername("user1")).thenReturn(Optional.empty());

        // when
        LoginResDto result = authService.login(req);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        // RefreshToken 저장 여부 확인
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenPort, times(1)).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsername()).isEqualTo("user1");
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("encoded-refresh-token");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 CommonException 발생")
    void login_fail_wrongPassword() {
        // given
        LoginReqDto req = new LoginReqDto("user1", "wrongPw");
        AuthUsers mockUser = AuthUsers.builder()
                .userId("user1")
                .password("encodedPw")
                .role(Role.ROLE_USER)
                .build();

        when(authUserPort.findByUserId("user1")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPw", "encodedPw")).thenReturn(false);

        // expect
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(CommonException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(refreshTokenPort, never()).save(any());
    }

    // ------------------------------------------------------------------------
    // 토큰 재발급
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("refresh 성공 시 새 토큰이 발급되고 저장된다")
    void refresh_success() {
        // given
        RefreshReqDto req = new RefreshReqDto("old-refresh-token");

        JwtInfo jwtInfo = new JwtInfo("user1", Role.ROLE_USER);
        RefreshToken existingToken = RefreshToken.builder()
                .username("user1")
                .token("encoded-old-refresh-token")
                .expiration(Instant.now().plusSeconds(3600))
                .build();

        when(jwtValidator.parseToken("old-refresh-token")).thenReturn(jwtInfo);
        when(refreshTokenPort.findByUsername("user1")).thenReturn(Optional.of(existingToken));
        when(encoder.matches("old-refresh-token", "encoded-old-refresh-token")).thenReturn(true);

        when(jwtIssuer.generateAccessToken("user1", Role.ROLE_USER)).thenReturn("new-access-token");
        when(jwtIssuer.generateRefreshToken("user1", Role.ROLE_USER))
                .thenReturn(new TokenInfo("new-refresh-token", Instant.now().plusSeconds(7200)));
        when(encoder.encode("new-refresh-token")).thenReturn("encoded-new-refresh-token");

        // when
        LoginResDto result = authService.refresh(req);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");

        // RefreshToken 저장 확인
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenPort, times(1)).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("encoded-new-refresh-token");
    }

    @Test
    @DisplayName("refresh 실패 - 토큰 불일치 시 CommonException 발생")
    void refresh_fail_tokenMismatch() {
        // given
        RefreshReqDto req = new RefreshReqDto("wrong-token");
        JwtInfo jwtInfo = new JwtInfo("user1", Role.ROLE_USER);

        RefreshToken existingToken = RefreshToken.builder()
                .username("user1")
                .token("encoded-refresh-token")
                .expiration(Instant.now().plusSeconds(3600))
                .build();

        when(jwtValidator.parseToken("wrong-token")).thenReturn(jwtInfo);
        when(refreshTokenPort.findByUsername("user1")).thenReturn(Optional.of(existingToken));
        when(encoder.matches("wrong-token", "encoded-refresh-token")).thenReturn(false);

        // expect
        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(CommonException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(refreshTokenPort, never()).save(any());
    }

    // ------------------------------------------------------------------------
    // 로그아웃
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("logout 시 refresh token 삭제가 수행된다")
    void logout_success() {
        // when
        String result = authService.logout("user1");

        // then
        verify(refreshTokenPort, times(1)).deleteByUsername("user1");
        assertThat(result).isEqualTo("로그아웃되었습니다.");
    }
}
