package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.AccountUrlResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final BankClient bankClient;
    @Value("${external.bank.account-url}")
    private String bankAccountUrl;

    public AccountUrlResDto getAccountUrl(String userId) {
        return AccountUrlResDto.builder()
                .accessToken(bankClient.requestBankToken(userId).accessToken())
                .url(bankAccountUrl)
                .build();
    }
}
