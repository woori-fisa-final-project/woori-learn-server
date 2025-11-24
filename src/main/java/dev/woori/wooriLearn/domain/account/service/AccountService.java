package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.AccountCreateReqDto;
import dev.woori.wooriLearn.domain.account.dto.AccountUrlResDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final BankClient bankClient;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Value("${external.bank.account-url}")
    private String bankAccountUrl;

    private static final String WOORI_BANK_CODE = "020";

    /**
     * userId를 토대로 은행의 url 및 url에 접근하기 위한 access token을 반환합니다.
     * @param userId 사용자 id
     * @return String url - 계좌개설 url / String accessToken
     */
    public AccountUrlResDto getAccountUrl(String userId) {
        return AccountUrlResDto.builder()
                .accessToken(bankClient.requestBankToken(userId).accessToken())
                .url(bankAccountUrl)
                .build();
    }

    /**
     * 요청에 담긴 정보를 토대로 계좌번호를 저장합니다.
     * @param request userId / accountNum / name(계좌주 실명)
     */
    public void registerAccount(AccountCreateReqDto request){
        // 사용자 찾기
        Users user = userRepository.findByUserId(request.userId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));

        // 계좌 엔티티 생성
        Account account = Account.builder()
                .accountName(request.name())
                .accountNumber(request.accountNum())
                .bankCode(WOORI_BANK_CODE)
                .user(user)
                .build();
        try {
            accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 등록된 계좌번호입니다.");
        }
    }
}
