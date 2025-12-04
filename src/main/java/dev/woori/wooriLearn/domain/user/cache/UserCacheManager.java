package dev.woori.wooriLearn.domain.user.cache;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.user.dto.UserInfoResDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCacheManager {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Cacheable(value = "userInfo_v2", key = "#userId", sync = true)
    public UserInfoResDto getUserInfoCached(String userId) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + userId
                ));

        String accountNumber = accountRepository.findByUserId(user.getId())
                .map(Account::getAccountNumber)
                .orElse("");

        return UserInfoResDto.builder()
                .nickname(user.getNickname())
                .point(user.getPoints())
                .account(accountNumber)
                .build();
    }
}
