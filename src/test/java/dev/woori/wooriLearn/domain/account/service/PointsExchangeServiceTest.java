package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.ExchangeProcessContext;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.dto.request.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsFailReason;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PointsExchangeServiceTest {

    @InjectMocks
    private PointsExchangeService service;

    @Mock
    private PointsHistoryRepository pointsHistoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;

    private Users user;
    private Account account;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2025-12-03T09:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // inject fixed clock
        service = new PointsExchangeService(fixedClock, pointsHistoryRepository, userRepository, accountRepository);

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
                .points(1_000)
                .build();

        account = Account.builder()
                .id(3L)
                .user(user)
                .accountNumber("123-456")
                .bankCode("020")
                .accountName("name")
                .build();
    }

    @Test
    @DisplayName("포인트 출금 신청 성공 시 요청 ID와 잔액을 반환한다")
    void requestExchange_success() {
        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(500, account.getAccountNumber(), "020");
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));

        PointsHistory saved = PointsHistory.builder()
                .id(11L)
                .user(user)
                .amount(dto.exchangeAmount())
                .status(PointsStatus.APPLY)
                .type(PointsHistoryType.WITHDRAW)
                .accountNumber(account.getAccountNumber())
                .build();
        when(pointsHistoryRepository.save(any(PointsHistory.class))).thenReturn(saved);

        PointsExchangeResponseDto res = service.requestExchange("user", dto);

        assertEquals(11L, res.requestId());
        assertEquals(PointsStatus.APPLY, res.status());
        assertEquals(500, res.exchangeAmount());
        assertEquals(500, res.currentBalance()); // 1000 - 500
        verify(pointsHistoryRepository).save(any());
    }

    @Test
    @DisplayName("출금 금액이 0 이하이면 INVALID_REQUEST 예외를 던진다")
    void requestExchange_invalidAmount() {
        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(0, account.getAccountNumber(), "020");
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange("user", dto));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("보유 포인트보다 큰 금액을 출금하면 CONFLICT 예외를 던진다")
    void requestExchange_insufficientPoints() {
        user = Users.builder()
                .id(1L)
                .authUser(user.getAuthUser())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .points(100)
                .build();
        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(500, account.getAccountNumber(), "020");
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange("user", dto));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌를 찾지 못하면 ENTITY_NOT_FOUND 예외를 던진다")
    void requestExchange_accountNotFound() {
        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(100, account.getAccountNumber(), "020");
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.empty());

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange("user", dto));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("다른 사용자의 계좌로 출금 요청 시 FORBIDDEN 예외를 던진다")
    void requestExchange_accountOwnedByOther_throwsForbidden() {
        PointsExchangeRequestDto dto = new PointsExchangeRequestDto(100, account.getAccountNumber(), "020");
        Users another = Users.builder()
                .id(99L)
                .authUser(user.getAuthUser())
                .userId("other")
                .nickname("other")
                .points(0)
                .build();
        Account otherAccount = Account.builder()
                .id(30L)
                .user(another)
                .accountNumber(account.getAccountNumber())
                .bankCode(account.getBankCode())
                .accountName(account.getAccountName())
                .build();
        when(userRepository.findByUserIdForUpdate("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(otherAccount));

        CommonException ex = assertThrows(CommonException.class, () -> service.requestExchange("user", dto));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("상태가 APPLY가 아니면 이체 준비에서 CONFLICT 예외를 던진다")
    void prepareTransfer_statusNotApply_throwsConflict() {
        PointsHistory history = PointsHistory.builder()
                .id(1L)
                .user(user)
                .amount(100)
                .status(PointsStatus.SUCCESS)
                .accountNumber(account.getAccountNumber())
                .build();
        when(pointsHistoryRepository.findAndLockById(1L)).thenReturn(Optional.of(history));

        CommonException ex = assertThrows(CommonException.class, () -> service.prepareTransfer(1L));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("이체 준비 성공 시 상태를 PROCESSING으로 바꾸고 컨텍스트를 반환한다")
    void prepareTransfer_success_marksProcessing() {
        PointsHistory history = PointsHistory.builder()
                .id(1L)
                .user(user)
                .amount(100)
                .status(PointsStatus.APPLY)
                .accountNumber(account.getAccountNumber())
                .build();
        when(pointsHistoryRepository.findAndLockById(1L)).thenReturn(Optional.of(history));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));

        ExchangeProcessContext ctx = service.prepareTransfer(1L);

        assertEquals(PointsStatus.PROCESSING, history.getStatus());
        assertEquals(account.getAccountNumber(), ctx.accountNum());
        assertEquals(100, ctx.amount());
    }

    @Test
    @DisplayName("계좌 이체 성공 응답을 받으면 상태를 SUCCESS로 변경한다")
    void processResult_successResponse_marksSuccess() {
        PointsHistory history = PointsHistory.builder()
                .id(1L)
                .user(user)
                .amount(200)
                .status(PointsStatus.PROCESSING)
                .accountNumber(account.getAccountNumber())
                .build();
        when(pointsHistoryRepository.findAndLockById(1L)).thenReturn(Optional.of(history));
        when(userRepository.findByUserIdForUpdate(user.getUserId())).thenReturn(Optional.of(user));

        BankTransferResDto bankRes = new BankTransferResDto(200, true, "ok", null);

        PointsExchangeResponseDto res = service.processResult(1L, bankRes);

        assertEquals(PointsStatus.SUCCESS, history.getStatus());
        assertNotNull(history.getProcessedAt());
        assertEquals(PointsStatus.SUCCESS, res.status());
    }

    @Test
    @DisplayName("이체 실패 시 상태를 FAILED로 두고 포인트를 환불한다")
    void processResult_failure_refundsPoints() {
        user = Users.builder()
                .id(1L)
                .authUser(user.getAuthUser())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .points(0)
                .build();
        PointsHistory history = PointsHistory.builder()
                .id(1L)
                .user(user)
                .amount(200)
                .status(PointsStatus.PROCESSING)
                .accountNumber(account.getAccountNumber())
                .build();
        when(pointsHistoryRepository.findAndLockById(1L)).thenReturn(Optional.of(history));
        when(userRepository.findByUserIdForUpdate(user.getUserId())).thenReturn(Optional.of(user));

        PointsExchangeResponseDto res = service.processResult(1L, null);

        assertEquals(PointsStatus.FAILED, history.getStatus());
        assertEquals(PointsFailReason.PROCESSING_ERROR, history.getFailReason());
        assertEquals(200, res.currentBalance());
        assertEquals(200, user.getPoints());
    }

    @Test
    @DisplayName("processFailure 호출 시 실패 상태로 전환하고 환불한다")
    void processFailure_marksFailedAndRefunds() {
        user = Users.builder()
                .id(1L)
                .authUser(user.getAuthUser())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .points(0)
                .build();
        PointsHistory history = PointsHistory.builder()
                .id(1L)
                .user(user)
                .amount(150)
                .status(PointsStatus.PROCESSING)
                .accountNumber(account.getAccountNumber())
                .build();
        when(pointsHistoryRepository.findAndLockById(1L)).thenReturn(Optional.of(history));
        when(userRepository.findByUserIdForUpdate(user.getUserId())).thenReturn(Optional.of(user));

        PointsExchangeResponseDto res = service.processFailure(1L);

        assertEquals(PointsStatus.FAILED, history.getStatus());
        assertEquals(PointsFailReason.PROCESSING_ERROR, history.getFailReason());
        assertEquals(150, user.getPoints());
        assertEquals(PointsStatus.FAILED, res.status());
    }

    @Test
    @DisplayName("대기 중인 출금 목록 조회는 기본 페이지 정보로 조회한다")
    void getPendingWithdrawals_usesDefaultPaging() {
        when(pointsHistoryRepository.findByTypeAndStatus(eq(PointsHistoryType.WITHDRAW), eq(PointsStatus.APPLY), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getPendingWithdrawals(null, null);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(pointsHistoryRepository).findByTypeAndStatus(eq(PointsHistoryType.WITHDRAW), eq(PointsStatus.APPLY), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("대기 중 출금 목록 조회 시 전달한 페이지 번호/크기를 사용한다")
    void getPendingWithdrawals_customPaging() {
        when(pointsHistoryRepository.findByTypeAndStatus(eq(PointsHistoryType.WITHDRAW), eq(PointsStatus.APPLY), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getPendingWithdrawals(2, 5);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(pointsHistoryRepository).findByTypeAndStatus(eq(PointsHistoryType.WITHDRAW), eq(PointsStatus.APPLY), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }
}
