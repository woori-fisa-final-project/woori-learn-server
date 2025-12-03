package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.AccountType;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.user.dto.UserInfoResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private AuthUserPort authUserPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PointsHistoryRepository pointsHistoryRepository;
    @Mock private EdubankapiAccountRepository eduAccountRepository;
    @Mock private AccountRepository accountRepository;

    private SignupReqDto signupReq;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        signupReq = new SignupReqDto("user1", "pw", "nick", "u@test.com");
        when(passwordEncoder.encode(any())).thenReturn("encoded");
    }

    @Test
    void signup_conflictUserId() {
        when(authUserPort.existsByUserId("user1")).thenReturn(true);
        CommonException ex = assertThrows(CommonException.class, () -> userService.signup(signupReq));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void signup_conflictEmail() {
        when(authUserPort.existsByUserId("user1")).thenReturn(false);
        when(userRepository.existsByEmail("u@test.com")).thenReturn(true);
        CommonException ex = assertThrows(CommonException.class, () -> userService.signup(signupReq));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void signup_success_createsAuthUserUserAccountsAndPoints() {
        when(authUserPort.existsByUserId("user1")).thenReturn(false);
        when(userRepository.existsByEmail("u@test.com")).thenReturn(false);
        when(eduAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(authUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> {
            Users u = inv.getArgument(0);
            u = Users.builder()
                    .id(10L)
                    .authUser(u.getAuthUser())
                    .userId(u.getUserId())
                    .nickname(u.getNickname())
                    .email(u.getEmail())
                    .points(u.getPoints())
                    .build();
            return u;
        });
        when(pointsHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eduAccountRepository.save(any(EducationalAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.signup(signupReq);

        ArgumentCaptor<PointsHistory> pointsCaptor = ArgumentCaptor.forClass(PointsHistory.class);
        verify(pointsHistoryRepository).save(pointsCaptor.capture());
        assertEquals(PointsHistoryType.DEPOSIT, pointsCaptor.getValue().getType());
        assertEquals(PointsStatus.SUCCESS, pointsCaptor.getValue().getStatus());

        verify(eduAccountRepository, times(2)).save(any(EducationalAccount.class));
    }

    @Test
    void getUserInfo_success() {
        Users user = Users.builder()
                .id(1L)
                .authUser(AuthUsers.builder().id(2L).userId("user1").password("p").role(Role.ROLE_USER).build())
                .userId("user1")
                .nickname("nick")
                .points(100)
                .build();
        when(userRepository.findByUserId("user1")).thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(Account.builder().id(3L).accountNumber("acc").build()));

        UserInfoResDto res = userService.getUserInfo("user1");
        assertEquals("nick", res.nickname());
        assertEquals(100, res.point());
        assertEquals("acc", res.account());
    }

    @Test
    void getUserInfo_notFound() {
        when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());
        CommonException ex = assertThrows(CommonException.class, () -> userService.getUserInfo("missing"));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void changeNickname_updatesAccounts() {
        Users user = Users.builder()
                .id(1L)
                .authUser(AuthUsers.builder().id(2L).userId("user1").password("p").role(Role.ROLE_USER).build())
                .userId("user1")
                .nickname("old")
                .points(0)
                .build();
        EducationalAccount acc1 = EducationalAccount.builder()
                .id(11L)
                .accountNumber("acc1")
                .accountType(AccountType.CHECKING)
                .balance(0)
                .accountPassword("pw")
                .accountName("old")
                .user(user)
                .build();
        when(userRepository.findByUserId("user1")).thenReturn(Optional.of(user));
        when(eduAccountRepository.findByUser_Id(1L)).thenReturn(List.of(acc1));

        userService.changeNickname("user1", new ChangeNicknameReqDto("newNick"));

        assertEquals("newNick", user.getNickname());
        assertEquals("newNick", acc1.getAccountName());
    }

    @Test
    void changeNickname_userNotFound() {
        when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());
        CommonException ex = assertThrows(CommonException.class,
                () -> userService.changeNickname("missing", new ChangeNicknameReqDto("n")));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }
}
