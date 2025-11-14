package dev.woori.wooriLearn.domain.user;

import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.repository.AuthUserRepository;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
import dev.woori.wooriLearn.domain.user.dto.ChangePasswdReqDto;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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


    @Test
    void testChangePassword() {
        AuthUsers auth = AuthUsers.builder()
                .userId("testUser")
                .password("oldPw")
                .role(Role.ROLE_USER)
                .build();

        when(authUserRepository.findByUserId("testUser"))
                .thenReturn(Optional.of(auth));
        when(passwordEncoder.encode("newPw"))
                .thenReturn("hashed");

        ChangePasswdReqDto req = new ChangePasswdReqDto("newPw");

        userService.changePassword("testUser", req);

        verify(authUserRepository).findByUserId("testUser");
        assertEquals("hashed", auth.getPassword());
    }
}
