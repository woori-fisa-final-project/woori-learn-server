package dev.woori.wooriLearn.domain.user;

import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioProgressResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.user.dto.AdminUserInfoResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import dev.woori.wooriLearn.domain.user.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private ScenarioProgressRepository scenarioProgressRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PointsHistoryRepository pointsHistoryRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private Users testUser;
    private Scenario scenario1;
    private Scenario scenario2;

    @BeforeEach
    void setup() {
        testUser = Users.builder()
                .id(1L)
                .userId("testUser")
                .nickname("테스트")
                .points(1000)
                .authUser(AuthUsers.builder().role(Role.ROLE_USER).build())
                .build();

        scenario1 = Scenario.builder().id(1L).title("시나리오1").build();
        scenario2 = Scenario.builder().id(2L).title("시나리오2").build();
    }

    @Test
    void testGetAdminUserInfo_AllCases() {
        // 1. repository mock 설정
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(scenarioRepository.findAll()).thenReturn(List.of(scenario1, scenario2));

        // 시나리오 진행률
        ScenarioProgress progress1 = ScenarioProgress.builder()
                .user(testUser)
                .scenario(scenario1)
                .progressRate(50.0)
                .build();
        ScenarioProgress progress2 = ScenarioProgress.builder()
                .user(testUser)
                .scenario(scenario2)
                .progressRate(0.0)
                .build();
        when(scenarioProgressRepository.findByUser(testUser))
                .thenReturn(List.of(progress1, progress2));

        // 계좌 없음
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // 포인트 내역
        PointsHistory ph1 = PointsHistory.builder()
                .user(testUser)
                .amount(500)
                .type(PointsHistoryType.DEPOSIT)
                .status(PointsStatus.SUCCESS)
                .build();

        PointsHistory ph2 = PointsHistory.builder()
                .user(testUser)
                .amount(200)
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.SUCCESS)
                .build();

        when(pointsHistoryRepository.findByUserId(1L)).thenReturn(List.of(ph1, ph2));

        // 2. 서비스 호출
        AdminUserInfoResDto dto = adminUserService.getAdminUserInfo(1L);

        // 3. 검증
        assertEquals(1L, dto.id());
        assertEquals("testUser", dto.userId());
        assertEquals(1000, dto.points());
        assertEquals(200, dto.exchangedPoints()); // 환전 성공 금액
        assertNull(dto.account()); // 계좌 없음
        assertEquals(2, dto.scenarios().size());

        // 전체 진행률: (50 + 0) / 2 = 25%
        assertEquals(25.0, dto.progressRate(), 0.01);

        // 시나리오 진행률 DTO 확인
        Map<Long, Double> progressMap = dto.scenarios().stream()
                .collect(Collectors.toMap(ScenarioProgressResDto::scenarioId, ScenarioProgressResDto::progressRate));
        assertEquals(50.0, progressMap.get(1L), 0.01);
        assertEquals(0.0, progressMap.get(2L), 0.01);

        // 포인트 내역 확인
        assertEquals(2, dto.pointHistories().size());
        assertTrue(dto.pointHistories().stream().anyMatch(p -> p.amount() == 500));
        assertTrue(dto.pointHistories().stream().anyMatch(p -> p.amount() == 200));
    }

    @Test
    void testAccountPresent() {
        Account account = Account.builder()
                .user(testUser)
                .accountNumber("123-456-789")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(scenarioRepository.findAll()).thenReturn(List.of());
        when(scenarioProgressRepository.findByUser(testUser)).thenReturn(List.of());
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(pointsHistoryRepository.findByUserId(1L)).thenReturn(List.of());

        AdminUserInfoResDto dto = adminUserService.getAdminUserInfo(1L);

        assertNotNull(dto.account());
        assertEquals("123-456-789", dto.account().accountNumber());
    }
}