package dev.woori.wooriLearn.domain.user;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
import dev.woori.wooriLearn.domain.auth.dto.ChangePasswdReqDto;
import dev.woori.wooriLearn.domain.user.dto.UserInfoResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import dev.woori.wooriLearn.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthUserPort authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @InjectMocks
    private AuthService authService;


    @Test
    void testGetUserInfo() {
        Users user = Users.builder()
                .userId("testUser")
                .nickname("nick")
                .points(300)
                .build();

        when(userRepository.findByUserId("testUser"))
                .thenReturn(Optional.of(user));

        UserInfoResDto result = userService.getUserInfo("testUser");

        assertEquals("nick", result.nickname());
        assertEquals(300, result.point());
    }


    @Test
    void testChangeNickname() {
        Users user = Users.builder()
                .userId("testUser")
                .nickname("oldNick")
                .points(100)
                .build();

        when(userRepository.findByUserId("testUser"))
                .thenReturn(Optional.of(user));

        ChangeNicknameReqDto req = new ChangeNicknameReqDto("newNick");

        userService.changeNickname("testUser", req);

        assertEquals("newNick", user.getNickname());
        verify(userRepository).findByUserId("testUser");
    }
}
