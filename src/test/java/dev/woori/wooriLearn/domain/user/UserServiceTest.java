package dev.woori.wooriLearn.domain.user;

import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
import dev.woori.wooriLearn.domain.user.dto.UserInfoResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import dev.woori.wooriLearn.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthUserPort authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PointsHistoryRepository pointsHistoryRepository;

    @Mock
    private EdubankapiAccountRepository eduAccountRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private UserService userService;

    @InjectMocks
    private AuthService authService;


    @Test
    @DisplayName("userId로 사용자와 계좌를 조회해 사용자 정보를 반환한다")
    void testGetUserInfo() {
        Users user = Users.builder()
                .id(1L)
                .userId("testUser")
                .nickname("nick")
                .points(300)
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .build();

        when(userRepository.findByUserId("testUser"))
                .thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(1L))
                .thenReturn(Optional.of(account));

        UserInfoResDto result = userService.getUserInfo("testUser");

        assertEquals("nick", result.nickname());
        assertEquals(300, result.point());
        assertEquals("1234567890", result.account());
    }


    @Test
    @DisplayName("닉네임을 변경하면 사용자와 교육계좌에 모두 반영된다")
    void testChangeNickname() {
        Users user = Users.builder()
                .id(1L)
                .userId("testUser")
                .nickname("oldNick")
                .points(100)
                .build();

        when(userRepository.findByUserId("testUser"))
                .thenReturn(Optional.of(user));
        when(eduAccountRepository.findByUser_Id(1L))
                .thenReturn(Collections.emptyList());

        ChangeNicknameReqDto req = new ChangeNicknameReqDto("newNick");

        userService.changeNickname("testUser", req);

        assertEquals("newNick", user.getNickname());
        verify(userRepository).findByUserId("testUser");
        verify(eduAccountRepository).findByUser_Id(1L);
    }
}
