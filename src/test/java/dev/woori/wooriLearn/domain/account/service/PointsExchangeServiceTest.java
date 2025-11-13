package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.common.HistoryStatus;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.PageResponse;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistorySearchRequestDto;
import dev.woori.wooriLearn.domain.account.entity.*;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Role;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PointsExchangeServiceTest {

    PointsHistoryRepository pointsHistoryRepository = mock(PointsHistoryRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    AccountRepository accountRepository = mock(AccountRepository.class);

    Clock fixedClock;
    PointsExchangeService service;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new PointsExchangeService(fixedClock, pointsHistoryRepository, userRepository, accountRepository);
    }

    private Users user(long id, int points) {
        return Users.builder()
                .id(id)
                .userId("user-" + id)
                .password("pw")
                .nickname("nick")
                .role(Role.ROLE_USER)
                .points(points)
                .build();
    }

    private Account account(long id, Users owner, String number) {
        return Account.builder()
                .id(id)
                .user(owner)
                .accountNumber(number)
                .bankCode("001")
                .accountName("계좌")
                .build();
    }

    @Test
    @DisplayName("현금화 신청 성공: WITHDRAW/APPLY 히스토리 저장")
    void requestExchange_success() {
        // given
        String username = "u1";
        Users u = user(1L, 1000);
        when(userRepository.findByUserIdForUpdate(username)).thenReturn(Optional.of(u));

        Account acc = account(10L, u, "123-456");
        when(accountRepository.findByAccountNumber("123-456")).thenReturn(Optional.of(acc));

        PointsHistory saved = PointsHistory.builder()
                .id(77L)
                .user(u)
                .amount(300)
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.APPLY)
                .build();
        when(pointsHistoryRepository.save(any(PointsHistory.class))).thenReturn(saved);

        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(300, "123-456", "001");

        // when
        PointsExchangeResponseDto res = service.requestExchange(username, dto);

        // then
        ArgumentCaptor<PointsHistory> captor = ArgumentCaptor.forClass(PointsHistory.class);
        verify(pointsHistoryRepository).save(captor.capture());
        PointsHistory toSave = captor.getValue();
        assertThat(toSave.getUser().getId()).isEqualTo(1L);
        assertThat(toSave.getAmount()).isEqualTo(300);
        assertThat(toSave.getType()).isEqualTo(PointsHistoryType.WITHDRAW);
        assertThat(toSave.getStatus()).isEqualTo(PointsStatus.APPLY);

        assertThat(res.requestId()).isEqualTo(77L);
        assertThat(res.userId()).isEqualTo(1L);
        assertThat(res.exchangeAmount()).isEqualTo(300);
        assertThat(res.status()).isEqualTo(PointsStatus.APPLY);
    }

    @Test
    @DisplayName("현금화 신청 실패: 잔액 부족(CONFLICT)")
    void requestExchange_insufficientPoints() {
        String username = "u1";
        Users u = user(1L, 100); // 부족
        when(userRepository.findByUserIdForUpdate(username)).thenReturn(Optional.of(u));

        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(300, "123-456", "001");

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange(username, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("현금화 신청 실패: 금액 null → INVALID_REQUEST (순서 수정 필요)")
    void requestExchange_invalidAmount_null() {
        String username = "u1";
        Users u = user(1L, 1000);
        when(userRepository.findByUserIdForUpdate(username)).thenReturn(Optional.of(u));

        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(null, "123-456", "001");

        // 현재 구현은 NPE 가능. 금액 검증을 먼저 하도록 서비스 수정 후 통과.
        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange(username, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("현금화 신청 실패: 금액 0/음수 → INVALID_REQUEST (순서 수정 권장)")
    void requestExchange_invalidAmount_nonPositive() {
        String username = "u1";
        Users u = user(1L, 1000);
        when(userRepository.findByUserIdForUpdate(username)).thenReturn(Optional.of(u));

        CommonException ex1 = assertThrows(CommonException.class,
                () -> service.requestExchange(username, new PointsExchangeRequestDto(0, "123-456", "001")));
        assertThat(ex1.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);

        CommonException ex2 = assertThrows(CommonException.class,
                () -> service.requestExchange(username, new PointsExchangeRequestDto(-10, "123-456", "001")));
        assertThat(ex2.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("현금화 신청 실패: 계좌 미존재 → ENTITY_NOT_FOUND")
    void requestExchange_accountNotFound() {
        String username = "u1";
        Users u = user(1L, 1000);
        when(userRepository.findByUserIdForUpdate(username)).thenReturn(Optional.of(u));
        when(accountRepository.findByAccountNumber("nope")).thenReturn(Optional.empty());

        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(100, "nope", "001");

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange(username, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    @DisplayName("현금화 신청 실패: 계좌 소유자 불일치 → FORBIDDEN")
    void requestExchange_accountOwnerMismatch() {
        String username = "u1";
        Users u = user(1L, 1000);
        when(userRepository.findByUserIdForUpdate(username)).thenReturn(Optional.of(u));

        Users other = user(2L, 500);
        Account acc = account(10L, other, "123-456");
        when(accountRepository.findByAccountNumber("123-456")).thenReturn(Optional.of(acc));

        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(100, "123-456", "001");

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange(username, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("현금화 승인 성공: SUCCESS, processedAt = fixedClock")
    void approveExchange_success() {
        // given history(APPLY) + user(points 충분)
        Users u = user(1L, 1000);
        PointsHistory history = PointsHistory.builder()
                .id(99L).user(u).amount(200)
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.APPLY)
                .build();

        when(pointsHistoryRepository.findById(99L)).thenReturn(Optional.of(history));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(u));
        when(pointsHistoryRepository.findAndLockById(99L)).thenReturn(Optional.of(history));
        when(pointsHistoryRepository.findAndLockById(anyLong())).thenReturn(Optional.of(history));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(u));
        when(userRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(u));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(u));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(u));

        // when
        PointsExchangeResponseDto res = service.approveExchange(99L);

        // then
        assertThat(history.getStatus()).isEqualTo(PointsStatus.SUCCESS);
        LocalDateTime expected = LocalDateTime.ofInstant(fixedClock.instant(), ZoneOffset.UTC);
        assertThat(history.getProcessedAt()).isEqualTo(expected);

        assertThat(res.status()).isEqualTo(PointsStatus.SUCCESS);
        assertThat(res.userId()).isEqualTo(1L);
        assertThat(res.exchangeAmount()).isEqualTo(200);
        assertThat(res.processedDate()).isEqualTo(expected);
    }

    @Test
    @DisplayName("현금화 승인 실패: APPLY 아님 → CONFLICT")
    void approveExchange_statusNotApply() {
        Users u = user(1L, 1000);
        PointsHistory history = PointsHistory.builder()
                .id(99L).user(u).amount(200)
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.SUCCESS) // 이미 처리됨
                .build();

        when(pointsHistoryRepository.findById(99L)).thenReturn(Optional.of(history));
        when(pointsHistoryRepository.findAndLockById(99L)).thenReturn(Optional.of(history));

        CommonException ex = assertThrows(CommonException.class, () -> service.approveExchange(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("현금화 승인 실패: 잔액 부족 → FAILED(INSUFFICIENT_POINTS)")
    void approveExchange_insufficientPoints_marksFailed() {
        Users u = user(1L, 100); // 부족
        PointsHistory history = PointsHistory.builder()
                .id(99L).user(u).amount(200)
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.APPLY)
                .build();

        when(pointsHistoryRepository.findById(99L)).thenReturn(Optional.of(history));
        when(pointsHistoryRepository.findAndLockById(99L)).thenReturn(Optional.of(history));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(u));

        PointsExchangeResponseDto res = service.approveExchange(99L);

        assertThat(history.getStatus()).isEqualTo(PointsStatus.FAILED);
        assertThat(res.status()).isEqualTo(PointsStatus.FAILED);
        assertThat(history.getFailReason()).isEqualTo(PointsFailReason.INSUFFICIENT_POINTS);
        assertThat(res.exchangeAmount()).isEqualTo(200);
        assertThat(res.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("현금화 승인 실패: 기타 처리 오류 → FAILED(PROCESSING_ERROR)")
    void approveExchange_processingError_marksFailed() {
        // Users.subtractPoints는 amount <= 0이면 INVALID_REQUEST 예외를 던짐
        Users u = user(1L, 1000);
        PointsHistory history = PointsHistory.builder()
                .id(99L).user(u).amount(0) // 비정상 데이터로 처리 오류 유도
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.APPLY)
                .build();

        when(pointsHistoryRepository.findById(99L)).thenReturn(Optional.of(history));
        when(pointsHistoryRepository.findAndLockById(99L)).thenReturn(Optional.of(history));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(u));

        PointsExchangeResponseDto res = service.approveExchange(99L);
        assertThat(history.getStatus()).isEqualTo(PointsStatus.FAILED);
        assertThat(history.getFailReason()).isEqualTo(PointsFailReason.PROCESSING_ERROR);
        assertThat(history.getProcessedAt()).isNotNull();
        assertThat(res.status()).isEqualTo(PointsStatus.FAILED);
        assertThat(res.processedDate()).isEqualTo(history.getProcessedAt());
        assertThat(u.getPoints()).isEqualTo(1000);
    }

    @Test
    @DisplayName("사용자의 포인트 내역 페이지 조회")
    void getUserHistoryPage_shouldReturnPagedResult() {
        // given
        String username = "testUser";
        Users user = user(1L, 1000);

        // 기본값 사용 (status=ALL, sort=DESC, page=1, size=20)
        PointsHistorySearchRequestDto requestDto = new PointsHistorySearchRequestDto(
                null, null, null, null, null, null, null, null
        );
        when(userRepository.findByUserId(username)).thenReturn(Optional.of(user));

        PointsHistory ph = PointsHistory.builder()
                .user(user)
                .amount(500)
                .status(PointsStatus.APPLY)
                .type(PointsHistoryType.WITHDRAW)
                .build();

        // ReflectionTestUtils로 BaseEntity 필드 세팅
        ReflectionTestUtils.setField(ph, "id", 100L);
        ReflectionTestUtils.setField(ph, "createdAt", LocalDateTime.now());

        Page<PointsHistory> pageResult = new PageImpl<>(List.of(ph));
        when(pointsHistoryRepository.findAllByFilters(
                any(), any(), any(), any(), any(), any()
        )).thenReturn(pageResult);

        // when
        PageResponse<PointsExchangeResponseDto> result =
                service.getUserHistoryPage(username, requestDto);

        // then
        assertNotNull(result);
        assertEquals(1, result.items().size());
        verify(pointsHistoryRepository, times(1))
                .findAllByFilters(eq(user.getId()), any(), any(), any(), any(), any(PageRequest.class));
    }

    @Test
    @DisplayName("관리자의 포인트 내역 페이지 조회")
    void getAdminHistoryPage_shouldReturnPagedResult() {
        // given - users
        Users user1 = user(1L, 1000);
        Users user2 = user(2L, 2000);

        userRepository.save(user1);
        userRepository.save(user2);

        // given - points history
        PointsHistory ph1 = PointsHistory.builder()
                .user(user1)
                .amount(200)
                .status(PointsStatus.SUCCESS)
                .type(PointsHistoryType.WITHDRAW)
                .build();
        PointsHistory ph2 = PointsHistory.builder()
                .user(user2)
                .amount(100)
                .status(PointsStatus.APPLY)
                .type(PointsHistoryType.WITHDRAW)
                .build();

        ReflectionTestUtils.setField(ph1, "id", 101L);
        ReflectionTestUtils.setField(ph1, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(ph2, "id", 102L);
        ReflectionTestUtils.setField(ph2, "createdAt", LocalDateTime.now());

        pointsHistoryRepository.save(ph1);
        pointsHistoryRepository.save(ph2);

        Page<PointsHistory> pageResult = new PageImpl<>(List.of(ph1, ph2));
        when(pointsHistoryRepository.findAllByFilters(
                any(), any(), any(), any(), any(), any()
        )).thenReturn(pageResult);

        PointsHistorySearchRequestDto requestDto = new PointsHistorySearchRequestDto(
                null, null, null, null, null, 1, 20, null // 전체 조회
        );

        // when
        PageResponse<PointsExchangeResponseDto> result =
                service.getAdminHistoryPage(requestDto);

        // then
        assertNotNull(result);
        assertEquals(2, result.items().size());

        // 항목 검증
        assertEquals(101L, result.items().get(0).requestId());
        assertEquals(102L, result.items().get(1).requestId());
    }
}
