package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.AccountDto;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor    // 의존성 주입하기 위해서 선언 final 필드를 인자로 받는 생성자로 자동 생성
public class AccountService {

    // 의존성 주입 대상
    private final AccountRepository accountRepository;

    /*
        사용자 ID를 통해 계좌 목록 조회

        - Repository를 통해 DB에서 특정 사용자{userId}의 계좌 데이터를 조회
        - Entity를 DTO로 변환하여 Controller에 전달

        @param userId : 사용자 ID
        @return 계좌 목록 : <List<AccountDto>>
     */
    public List<AccountDto> getAccountByUserId(long userId) {

        // Repository 호출을 통해 educationl_account 테이블에 user_id가 일치하는 계좌 엔티티 목록 조회
        List<EducationalAccount> accounts = accountRepository.findByUserId(userId);

        // 사용자 ID가 없으면 404 응답 처리
        if (accounts.isEmpty()) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "해당 사용자의 계좌를 찾을 수 없습니다.");
        }

        return accounts.stream()
                .map(AccountDto::from)
                .collect(Collectors.toList());
    }
}
