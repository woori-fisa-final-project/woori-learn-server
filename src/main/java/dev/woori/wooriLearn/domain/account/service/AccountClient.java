package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.external.request.ExternalAccountCheckReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.request.ExternalAccountUrlReqDto;
import dev.woori.wooriLearn.domain.account.dto.external.response.ExternalAccountUrlResDto;
import dev.woori.wooriLearn.domain.account.dto.response.AccountCreateResDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// 은행 서버로 보내는 요청들 관리
@HttpExchange
public interface AccountClient {

    @PostExchange("/account/tid")
    ExternalAccountUrlResDto getAccountUrl(@RequestBody ExternalAccountUrlReqDto externalAccountUrlReqDto);

    @PostExchange("/account/lookup")
    AccountCreateResDto getAccountNum(@RequestBody ExternalAccountCheckReqDto accountCheckReqDto);
}
