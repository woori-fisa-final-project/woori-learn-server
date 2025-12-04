package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.ExchangeProcessContext;
import dev.woori.wooriLearn.domain.account.dto.external.request.BankTransferReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PointsExchangeFacadeTest {

    @InjectMocks
    private PointsExchangeFacade facade;

    @Mock
    private PointsExchangeService pointsExchangeService;

    @Mock
    private AccountClient accountClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(facade, "adminAccountNumber", "ADMIN-ACC");
    }

    @Test
    @DisplayName("이체 준비 후 은행 API 성공 시 결과를 처리하고 요청을 완료한다")
    void executeTransfer_successfulBankCall() {
        ExchangeProcessContext ctx = ExchangeProcessContext.builder()
                .requestId(1L)
                .userId(2L)
                .accountNum("USER-ACC")
                .amount(1000)
                .build();
        when(pointsExchangeService.prepareTransfer(1L)).thenReturn(ctx);
        BankTransferResDto bankRes = new BankTransferResDto(200, true, "ok", null);
        when(accountClient.transfer(any(BankTransferReqDto.class))).thenReturn(bankRes);
        PointsExchangeResponseDto expected = PointsExchangeResponseDto.builder().build();
        when(pointsExchangeService.processResult(1L, bankRes)).thenReturn(expected);

        PointsExchangeResponseDto res = facade.executeTransfer(1L);

        assertEquals(expected, res);
        ArgumentCaptor<BankTransferReqDto> captor = ArgumentCaptor.forClass(BankTransferReqDto.class);
        verify(accountClient).transfer(captor.capture());
        assertEquals("ADMIN-ACC", captor.getValue().fromAccount());
        assertEquals("USER-ACC", captor.getValue().toAccount());
        assertEquals(1000L, captor.getValue().amount());
    }

    @Test
    @DisplayName("은행 API 실패 시 processFailure로 상태를 실패로 전환한다")
    void executeTransfer_bankFailure_callsProcessFailure() {
        ExchangeProcessContext ctx = ExchangeProcessContext.builder()
                .requestId(2L)
                .userId(2L)
                .accountNum("USER-ACC")
                .amount(2000)
                .build();
        when(pointsExchangeService.prepareTransfer(2L)).thenReturn(ctx);
        when(accountClient.transfer(any(BankTransferReqDto.class))).thenThrow(new RestClientException("fail"));
        PointsExchangeResponseDto expected = PointsExchangeResponseDto.builder().build();
        when(pointsExchangeService.processFailure(2L)).thenReturn(expected);

        PointsExchangeResponseDto res = facade.executeTransfer(2L);

        assertEquals(expected, res);
        verify(pointsExchangeService).processFailure(2L);
    }
}
