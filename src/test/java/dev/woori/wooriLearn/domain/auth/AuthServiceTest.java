package dev.woori.wooriLearn.domain.auth;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.config.security.Encoder;
import dev.woori.wooriLearn.domain.auth.dto.ChangePasswdReqDto;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    private AuthUserPort authUserPort;
    private RefreshTokenPort refreshTokenPort;
    private PasswordEncoder passwordEncoder;
    private Encoder encoder;
    private JwtIssuer jwtIssuer;
    private JwtValidator jwtValidator;
    private AuthService authService;

    private String testUserId = "test";
    private String testPassword = "1234";

    @BeforeEach
    void setUp() {
        authUserPort = mock(AuthUserPort.class);
        refreshTokenPort = mock(RefreshTokenPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        encoder = mock(Encoder.class);
        jwtIssuer = mock(JwtIssuer.class);
        jwtValidator = mock(JwtValidator.class);

        authService = new AuthService(authUserPort, passwordEncoder, encoder, jwtIssuer, jwtValidator, refreshTokenPort);

        // 실패 케이스의 never() 검증과 충돌하지 않도록 암호화 호출 없이 더미 사용자만 저장
        AuthUsers user = AuthUsers.builder()
                .userId(testUserId)
                .password("hashedPassword1234")  // 미리 인코딩된 값으로 가정
                .role(Role.ROLE_USER)
                .build();
        authUserPort.save(user);

    }

    @Test
    void testChangePassword() {
        // 정상 흐름: 현재 비밀번호 일치 → 새 비밀번호로 업데이트
        AuthUsers auth = AuthUsers.builder()
                .userId("testUser")
                .password("oldHashedPw")
                .role(Role.ROLE_USER)
                .build();

        ChangePasswdReqDto req = new ChangePasswdReqDto("currentPw", "newPw");

        when(authUserPort.findByUserId("testUser"))
                .thenReturn(Optional.of(auth));
        when(passwordEncoder.matches("currentPw", "oldHashedPw"))
                .thenReturn(true);
        when(passwordEncoder.encode("newPw"))
                .thenReturn("newHashedPw");

        // when
        authService.changePassword("testUser", req);

        // then
        verify(authUserPort).findByUserId("testUser");
        verify(passwordEncoder).matches("currentPw", "oldHashedPw");
        verify(passwordEncoder).encode("newPw");
        assertEquals("newHashedPw", auth.getPassword());
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        // 실패 흐름: 현재 비밀번호 불일치 → 예외, encode 호출 없음
        AuthUsers auth = AuthUsers.builder()
                .userId("testUser")
                .password("oldHashedPw")
                .role(Role.ROLE_USER)
                .build();

        ChangePasswdReqDto req = new ChangePasswdReqDto("wrongPw", "newPw");

        when(authUserPort.findByUserId("testUser"))
                .thenReturn(Optional.of(auth));
        when(passwordEncoder.matches("wrongPw", "oldHashedPw"))
                .thenReturn(false);

        // expect
        assertThrows(CommonException.class,
                () -> authService.changePassword("testUser", req));

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void testChangePassword_UserNotFound() {
        // 실패 흐름: 사용자 미존재 → 예외, matches/encode 호출 없음
        ChangePasswdReqDto req = new ChangePasswdReqDto("currentPw", "newPw");

        when(authUserPort.findByUserId("nonExistUser"))
                .thenReturn(Optional.empty());

        assertThrows(CommonException.class,
                () -> authService.changePassword("nonExistUser", req));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode(any());
    }
}
