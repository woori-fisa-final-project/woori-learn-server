package dev.woori.wooriLearn.domain.user;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import dev.woori.wooriLearn.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class SignupTest {
    private UserRepository userRepository;
    private AuthUserPort authUserRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private PointsHistoryRepository pointsHistoryRepository;
    private EdubankapiAccountRepository eduAccountRepository;
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authUserRepository = mock(AuthUserPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        pointsHistoryRepository = mock(PointsHistoryRepository.class);
        eduAccountRepository = mock(EdubankapiAccountRepository.class);
        userService = new UserService(userRepository, authUserRepository, passwordEncoder, pointsHistoryRepository, eduAccountRepository, accountRepository);
    }

    @Test
    @DisplayName("회원가입 성공 시 AuthUsers와 Users가 각각 저장된다")
    void signup_success() {
        // given
        SignupReqDto req = new SignupReqDto("user1", "rawPassword", "nickname");
        when(authUserRepository.existsByUserId("user1")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        // when
        userService.signup(req);

        // then
        ArgumentCaptor<AuthUsers> authCaptor = ArgumentCaptor.forClass(AuthUsers.class);
        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        ArgumentCaptor<PointsHistory> historyCaptor = ArgumentCaptor.forClass(PointsHistory.class);

        verify(authUserRepository, times(1)).save(authCaptor.capture());
        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(pointsHistoryRepository, times(1)).save(historyCaptor.capture());

        AuthUsers savedAuth = authCaptor.getValue();
        Users savedUser = userCaptor.getValue();
        PointsHistory savedHistory = historyCaptor.getValue();

        assertThat(savedAuth.getUserId()).isEqualTo("user1");
        assertThat(savedAuth.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedAuth.getRole()).isEqualTo(Role.ROLE_USER);

        assertThat(savedUser.getAuthUser()).isEqualTo(savedAuth);
        assertThat(savedUser.getUserId()).isEqualTo("user1");
        assertThat(savedUser.getNickname()).isEqualTo("nickname");
        assertThat(savedUser.getPoints()).isEqualTo(1000);

        assertThat(savedHistory.getUser()).isEqualTo(savedUser);
        assertThat(savedHistory.getAmount()).isEqualTo(1000);
        assertThat(savedHistory.getType()).isEqualTo(PointsHistoryType.DEPOSIT);
        assertThat(savedHistory.getStatus()).isEqualTo(PointsStatus.SUCCESS);
    }

    @Test
    @DisplayName("이미 존재하는 아이디로 회원가입 시 CommonException 발생")
    void signup_conflict() {
        // given
        SignupReqDto req = new SignupReqDto("duplicate", "pw", "nick");
        when(authUserRepository.existsByUserId("duplicate")).thenReturn(true);

        // expect
        assertThatThrownBy(() -> userService.signup(req))
                .isInstanceOf(CommonException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONFLICT);

        verify(authUserRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
