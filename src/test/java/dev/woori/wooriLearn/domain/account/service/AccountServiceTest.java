package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.AccountInfo;
import dev.woori.wooriLearn.domain.account.dto.SessionIdData;
import dev.woori.wooriLearn.domain.account.dto.external.request.ExternalAccountCheckReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.ExternalAccountUrlResDto;
import dev.woori.wooriLearn.domain.account.dto.request.AccountCreateReqDto;
import dev.woori.wooriLearn.domain.account.dto.response.AccountCreateResDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.AccountSession;
import dev.woori.wooriLearn.domain.account.entity.AccountStoreRedis;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountClient accountClient;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountStoreRedis redis;

    private static final String USER_ID = "user-1";
    private static final String TID = "tid-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(accountService, "bankAccountUrl", "https://bank.example.com");
    }

    @Test
    @DisplayName("계좌 등록 URL을 받아 세션을 저장하고 응답을 반환한다")
    void getAccountUrl_success_savesSessionAndReturnsUrl() {
        ExternalAccountUrlResDto response = ExternalAccountUrlResDto.builder()
                .code("200")
                .message("ok")
                .data(new SessionIdData(TID))
                .build();
        when(accountClient.getAccountUrl()).thenReturn(response);

        var result = accountService.getAccountUrl(USER_ID);

        assertEquals("https://bank.example.com", result.url());
        assertEquals(TID, result.tid());

        ArgumentCaptor<AccountSession> captor = ArgumentCaptor.forClass(AccountSession.class);
        verify(redis, times(1)).save(eq(TID), captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
    }

    @Test
    @DisplayName("TID가 비어 있으면 EXTERNAL_API_FAIL 예외를 던진다")
    void getAccountUrl_tidNull_throwsExternalApiFail() {
        ExternalAccountUrlResDto response = ExternalAccountUrlResDto.builder()
                .code("200")
                .message("ok")
                .data(new SessionIdData(null))
                .build();
        when(accountClient.getAccountUrl()).thenReturn(response);

        CommonException ex = assertThrows(CommonException.class, () -> accountService.getAccountUrl(USER_ID));
        assertEquals(ErrorCode.EXTERNAL_API_FAIL, ex.getErrorCode());
        verify(redis, never()).save(any(), any());
    }

    @Test
    @DisplayName("계좌 URL 요청이 401이면 UNAUTHORIZED 예외를 던진다")
    void getAccountUrl_http401_throwsUnauthorized() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "unauth", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(accountClient.getAccountUrl()).thenThrow(exception);

        CommonException ex = assertThrows(CommonException.class, () -> accountService.getAccountUrl(USER_ID));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("기타 4xx 응답이면 EXTERNAL_API_FAIL 예외를 던진다")
    void getAccountUrl_other4xx_throwsExternalApiFail() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(accountClient.getAccountUrl()).thenThrow(exception);

        CommonException ex = assertThrows(CommonException.class, () -> accountService.getAccountUrl(USER_ID));
        assertEquals(ErrorCode.EXTERNAL_API_FAIL, ex.getErrorCode());
    }

    @Test
    @DisplayName("RestClientException 발생 시 EXTERNAL_API_FAIL 예외를 던진다")
    void getAccountUrl_restClientException_throwsExternalApiFail() {
        when(accountClient.getAccountUrl()).thenThrow(new RestClientException("fail"));

        CommonException ex = assertThrows(CommonException.class, () -> accountService.getAccountUrl(USER_ID));
        assertEquals(ErrorCode.EXTERNAL_API_FAIL, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌 등록 성공 시 계좌를 저장하고 세션을 삭제한다")
    void registerAccount_success_savesAccountAndDeletesSession() {
        AccountSession session = AccountSession.builder().userId(USER_ID).build();
        when(redis.get(TID)).thenReturn(session);

        Users user = Users.builder()
                .id(10L)
                .authUser(AuthUsers.builder()
                        .id(20L)
                        .userId(USER_ID)
                        .password("pw")
                        .role(Role.ROLE_USER)
                        .build())
                .userId(USER_ID)
                .nickname("nick")
                .points(0)
                .build();
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID)))
                .thenReturn(new AccountCreateResDto("200", "ok", new AccountInfo("name", "1234")));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertEquals("1234", saved.getAccountNumber());
        assertEquals("020", saved.getBankCode());
        assertEquals(user, saved.getUser());
        verify(redis).delete(TID);
    }

    @Test
    @DisplayName("등록 세션이 없으면 FORBIDDEN 예외를 던진다")
    void registerAccount_noSession_throwsForbidden() {
        when(redis.get(TID)).thenReturn(null);

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(userRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("세션의 userId가 다르면 FORBIDDEN 예외를 던진다")
    void registerAccount_userMismatch_throwsForbidden() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId("another").build());

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(userRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("사용자를 찾지 못하면 ENTITY_NOT_FOUND 예외를 던진다")
    void registerAccount_userNotFound_throwsEntityNotFound() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("은행 API 응답 data가 없으면 EXTERNAL_API_FAIL 예외를 던진다")
    void registerAccount_nullData_throwsExternalApiFail() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));

        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID)))
                .thenReturn(new AccountCreateResDto("200", "ok", null));

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.EXTERNAL_API_FAIL, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌 저장 중 무결성 예외가 나면 CONFLICT 예외를 던진다")
    void registerAccount_dataIntegrityViolation_throwsConflict() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));
        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID)))
                .thenReturn(new AccountCreateResDto("200", "ok", new AccountInfo("name", "1234")));
        when(accountRepository.save(any(Account.class))).thenThrow(new DataIntegrityViolationException("dup"));

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌 등록 시 401 응답이면 UNAUTHORIZED 예외를 던진다")
    void registerAccount_http401_throwsUnauthorized() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "unauth", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID))).thenThrow(exception);

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌 등록 시 404 응답이면 ENTITY_NOT_FOUND 예외를 던진다")
    void registerAccount_http404_throwsEntityNotFound() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "not found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID))).thenThrow(exception);

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌 등록 시 400 응답이면 INVALID_REQUEST 예외를 던진다")
    void registerAccount_http400_throwsInvalidRequest() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID))).thenThrow(exception);

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("기타 4xx 응답이면 EXTERNAL_API_FAIL 예외를 던진다")
    void registerAccount_other4xx_throwsExternalApiFail() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "too many", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID))).thenThrow(exception);

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.EXTERNAL_API_FAIL, ex.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 RestClientException 발생 시 EXTERNAL_API_FAIL 예외를 던진다")
    void registerAccount_restClientException_throwsExternalApiFail() {
        when(redis.get(TID)).thenReturn(AccountSession.builder().userId(USER_ID).build());
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(dummyUser()));
        when(accountClient.getAccountNum(new ExternalAccountCheckReqDto(TID)))
                .thenThrow(new RestClientException("fail"));

        CommonException ex = assertThrows(CommonException.class,
                () -> accountService.registerAccount(USER_ID, new AccountCreateReqDto(TID)));
        assertEquals(ErrorCode.EXTERNAL_API_FAIL, ex.getErrorCode());
    }

    private Users dummyUser() {
        return Users.builder()
                .id(10L)
                .authUser(AuthUsers.builder()
                        .id(20L)
                        .userId(USER_ID)
                        .password("pw")
                        .role(Role.ROLE_USER)
                        .build())
                .userId(USER_ID)
                .nickname("nick")
                .points(0)
                .build();
    }
}
