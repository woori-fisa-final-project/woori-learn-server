package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.AccountAuthReqDto;
import dev.woori.wooriLearn.domain.account.dto.AccountAuthVerifyReqDto;
import dev.woori.wooriLearn.domain.account.entity.AccountAuth;
import dev.woori.wooriLearn.domain.account.repository.AccountAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AccountAuthServiceTest {

    @Mock AccountAuthRepository repository;
    @Mock ExternalAuthClient externalAuthClient;

    @InjectMocks AccountAuthService service;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    @DisplayName("기존 레코드가 없으면 신규 생섬")
    void request_whenNoExistingRow_createsNewAndSaves() {
        // given
        String userId = "U1";
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(externalAuthClient.requestOtp(anyString(), anyString(), anyString()))
                .thenReturn("123456");

        AccountAuthReqDto req = new AccountAuthReqDto(
                userId,
                "pass",
                "김철수",
                "01022222222",
                "040101-3"
        );

        // when
        service.request(userId, req);

        // then
        ArgumentCaptor<AccountAuth> captor = ArgumentCaptor.forClass(AccountAuth.class);
        verify(repository).save(captor.capture());
        AccountAuth saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("U1");
        assertThat(saved.getAuthCode()).isEqualTo("123456");
    }

    @Test
    @DisplayName("기존 레코드가 있으면 덮어쓰기")
    void request_whenExistingRow_updatesCode() {
        // given
        String userId = "U1";
        AccountAuth existing = AccountAuth.builder()
                .id(10L).userId(userId).authCode("000000").build();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(externalAuthClient.requestOtp(anyString(), anyString(), anyString()))
                .thenReturn("654321");

        AccountAuthReqDto req = new AccountAuthReqDto(
                userId,
                "pass",
                "김철수",
                "01022222222",
                "040101-3"
        );

        // when
        service.request(userId, req);

        // then
        assertThat(existing.getAuthCode()).isEqualTo("654321");
        verify(repository).findByUserId("U1");
        verify(externalAuthClient).requestOtp(anyString(), anyString(), anyString());
        verify(repository, never()).save(any(AccountAuth.class));
    }

    @Test
    @DisplayName("인증 코드 일치 시 레코드 삭제")
    void verify_whenCodeMatches_andDeletesRow() {
        // given
        String userId = "U1";
        AccountAuth row = AccountAuth.builder().userId(userId).authCode("777777").build();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(row));

        AccountAuthVerifyReqDto req = new AccountAuthVerifyReqDto("U1", "777777");

        // when
        service.verify(userId, req);

        // then
        verify(repository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("인증 코드 불일치 시 에러")
    void verify_whenCodeNotMatch_andNotDelete() {
        // given
        String userId = "U1";
        AccountAuth row = AccountAuth.builder().userId(userId).authCode("111111").build();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(row));

        AccountAuthVerifyReqDto req = new AccountAuthVerifyReqDto("U1", "000000");

        // when & then
        CommonException ex = assertThrows(CommonException.class, () -> service.verify(userId, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(repository, never()).deleteByUserId(anyString());
    }

    @Test
    @DisplayName("인증 번호 요청 이력이 없으면 예외 처리")
    void verify_whenNoRow_throwsCommonException() {
        when(repository.findByUserId(anyString())).thenReturn(Optional.empty());
        assertThrows(CommonException.class,
                () -> service.verify("U1", new AccountAuthVerifyReqDto("U1", "123456")));
    }
}