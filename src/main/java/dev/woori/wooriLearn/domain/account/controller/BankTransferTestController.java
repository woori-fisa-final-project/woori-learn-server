package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.domain.account.dto.external.request.BankTransferReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.BankTransferResDto;
import dev.woori.wooriLearn.domain.account.service.AccountClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/bank")
public class BankTransferTestController {

    private final AccountClient accountClient;

    @PostMapping("/transfer")
    public BankTransferResDto testTransfer() {
        BankTransferReqDto req = new BankTransferReqDto(
                "999900000001",
                "111122223333",
                100000L
        );

        return accountClient.transfer(req);
    }
}
