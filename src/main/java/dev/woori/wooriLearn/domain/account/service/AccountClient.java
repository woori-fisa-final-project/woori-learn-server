package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.AccountCreateReqDto;
import dev.woori.wooriLearn.domain.account.dto.AccountCreateResDto;
import dev.woori.wooriLearn.domain.account.dto.BankTokenReqDto;
import dev.woori.wooriLearn.domain.account.dto.BankTokenResDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// 은행 서버로 보내는 요청들 관리
@HttpExchange
public interface AccountClient {

    @PostExchange("/auth/token")
    BankTokenResDto getAccountUrl(@RequestBody BankTokenReqDto bankTokenReqDto);

    @PostExchange("/account/lookup")
    AccountCreateResDto getAccountNum(@RequestBody AccountCreateReqDto accountCreateReqDto);
}
