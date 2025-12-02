package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.ExchangeProcessContext;
import dev.woori.wooriLearn.domain.account.dto.external.request.BankTransferReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsExchangeFacade {

    private final PointsExchangeService pointsExchangeService;
    private final AccountClient accountClient;

    @Value("${app.admin.account-number}")
    private String adminAccountNumber;

    public PointsExchangeResponseDto executeTransfer(Long requestId) {

        ExchangeProcessContext context = pointsExchangeService.prepareTransfer(requestId);
        BankTransferResDto bankRes;

        try {
            BankTransferReqDto bankReq = new BankTransferReqDto(
                    adminAccountNumber,
                    context.accountNum(),
                    context.amount()
            );
           bankRes = accountClient.transfer(bankReq);
        } catch (RestClientException e) {
            // Processing 상태 유지
            log.error("은행 서버 통신 오류. requestId={}", requestId, e);
            return pointsExchangeService.processFailure(requestId);
        }
        return pointsExchangeService.processResult(requestId, bankRes);
    }
}
