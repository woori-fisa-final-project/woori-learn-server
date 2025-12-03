package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.common.SearchPeriod;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.request.PointsUnifiedHistoryRequestDto;
import dev.woori.wooriLearn.domain.account.entity.HistoryFilter;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryQueryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointsHistoryServiceTest {

    @InjectMocks
    private PointsHistoryService service;

    @Mock
    private PointsHistoryQueryRepository queryRepository;
    @Mock
    private UserRepository userRepository;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2025-12-03T09:00:00Z"), ZoneId.of("UTC"));

    private Users user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PointsHistoryService(queryRepository, userRepository, fixedClock);
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
    @DisplayName("비관리자 요청은 userId로 사용자 조회 후 필터링하여 조회한다")
    void getUnifiedHistory_userLookupForNonAdmin() {
        when(userRepository.findByUserId("user")).thenReturn(Optional.of(user));
        when(queryRepository.findAllByFilters(any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PointsUnifiedHistoryRequestDto req = new PointsUnifiedHistoryRequestDto(
                null, null, SearchPeriod.MONTH, SortDirection.ASC, HistoryFilter.WITHDRAW_SUCCESS, 1, 10, null);

        service.getUnifiedHistory("user", req, false);

        verify(queryRepository).findAllByFilters(
                eq(user.getId()),
                eq(PointsHistoryType.WITHDRAW),
                eq(PointsStatus.SUCCESS),
                any(),
                any(),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("관리자 요청은 사용자 필터 없이 전체 조회한다")
    void getUnifiedHistory_adminCanSkipUserFilter() {
        when(queryRepository.findAllByFilters(any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(Page.empty());

        PointsUnifiedHistoryRequestDto req = new PointsUnifiedHistoryRequestDto(
                null, null, SearchPeriod.ALL, SortDirection.DESC, HistoryFilter.ALL, 1, 20, null);

        service.getUnifiedHistory("admin", req, true);

        verify(queryRepository).findAllByFilters(
                eq(null),
                eq(null),
                eq(null),
                any(),
                any(),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("다른 사용자의 기록을 비관리자가 요청하면 FORBIDDEN 예외를 던진다")
    void getUnifiedHistory_nonAdminRequestingOtherUser_forbidden() {
        PointsUnifiedHistoryRequestDto req = new PointsUnifiedHistoryRequestDto(
                null, null, SearchPeriod.ALL, SortDirection.DESC, HistoryFilter.ALL, 1, 20, 99L);

        CommonException ex = assertThrows(CommonException.class, () -> service.getUnifiedHistory("user", req, false));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("startDate 파싱 실패 시 INVALID_REQUEST 예외를 던진다")
    void getUnifiedHistory_invalidStartDate_throws() {
        PointsUnifiedHistoryRequestDto req = new PointsUnifiedHistoryRequestDto(
                "invalid-date", null, SearchPeriod.ALL, SortDirection.DESC, HistoryFilter.ALL, 1, 20, null);

        CommonException ex = assertThrows(CommonException.class, () -> service.getUnifiedHistory("user", req, true));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("endDate만 주어지면 한 달 이전을 start로 간주해 조회한다")
    void getUnifiedHistory_onlyEndDate_setsStartFromEndMinusMonth() {
        when(queryRepository.findAllByFilters(any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(Page.empty());
        when(userRepository.findByUserId("user")).thenReturn(Optional.of(user));
        PointsUnifiedHistoryRequestDto req = new PointsUnifiedHistoryRequestDto(
                null, "2025-11-30", SearchPeriod.ALL, SortDirection.DESC, HistoryFilter.DEPOSIT, 1, 20, null);

        service.getUnifiedHistory("user", req, false);

        verify(queryRepository).findAllByFilters(
                eq(user.getId()),
                eq(PointsHistoryType.DEPOSIT),
                eq(null),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(PageRequest.class)
        );
    }
}
