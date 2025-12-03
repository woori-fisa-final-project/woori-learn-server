package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.request.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class PointsDepositServiceTest {

    @InjectMocks
    private PointsDepositService service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsHistoryRepository pointsHistoryRepository;

    private Users user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = Users.builder()
                .id(1L)
                .authUser(AuthUsers.builder()
                        .id(2L)
                        .userId("user")
                        .password("pw")
                        .role(Role.ROLE_USER)
                        .build())
                .userId("user")
                .nickname("nick")
                .points(0)
                .build();
    }

    @Test
    @DisplayName("포인트 충전 성공 시 히스토리를 저장하고 잔액을 반환한다")
    void depositPoints_success() {
        PointsDepositRequestDto dto = new PointsDepositRequestDto(100, "reason");
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));

        PointsHistory saved = PointsHistory.builder()
                .id(10L)
                .user(user)
                .amount(100)
                .type(PointsHistoryType.DEPOSIT)
                .status(PointsStatus.SUCCESS)
                .build();
        // give createdAt for response
        saved.getClass(); // noop to avoid unused warning
        when(pointsHistoryRepository.save(any(PointsHistory.class))).thenReturn(saved);

        var res = service.depositPoints("user", dto);

        ArgumentCaptor<PointsHistory> captor = ArgumentCaptor.forClass(PointsHistory.class);
        verify(pointsHistoryRepository).save(captor.capture());
        assertEquals(100, captor.getValue().getAmount());
        assertEquals(PointsHistoryType.DEPOSIT, captor.getValue().getType());
        assertEquals(PointsStatus.SUCCESS, res.status());
        assertEquals(100, res.addedPoint());
        assertEquals(100, res.currentBalance());
        assertEquals("reason", res.message());
    }

    @Test
    @DisplayName("사용자를 찾지 못하면 ENTITY_NOT_FOUND 예외를 던진다")
    void depositPoints_userNotFound() {
        when(userRepository.findByUserIdForUpdate("missing")).thenReturn(Optional.empty());

        CommonException ex = assertThrows(CommonException.class,
                () -> service.depositPoints("missing", new PointsDepositRequestDto(100, null)));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("충전 금액이 0 이하이면 INVALID_REQUEST 예외를 던진다")
    void depositPoints_invalidAmount_throwsCommonException() {
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));

        CommonException ex = assertThrows(CommonException.class,
                () -> service.depositPoints("user", new PointsDepositRequestDto(0, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }
}
