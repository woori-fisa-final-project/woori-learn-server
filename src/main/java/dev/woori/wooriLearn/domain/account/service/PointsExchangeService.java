package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.WithdrawRequestDto;
import dev.woori.wooriLearn.domain.account.dto.WithdrawResponseDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.WithdrawRequest;
import dev.woori.wooriLearn.domain.account.entity.WithdrawStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.WithdrawRequestRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final WithdrawRequestRepository withdrawRequestRepository;
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;

    public WithdrawResponseDto requestWithdraw(WithdrawRequestDto dto) {

        // ✅ 1. 유저 검증
        Users user = usersRepository.findById(dto.getDb_id())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // ✅ 2. 계좌 검증 (계좌 없음 → 환전 신청 불가)
        Account account = accountRepository.findByAccountNumber(dto.getAccountNum())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));

        // ✅ 3. 출금 신청 저장
        WithdrawRequest req = withdrawRequestRepository.save(
                WithdrawRequest.builder()
                        .user(user)
                        .withdrawAmount(dto.getWithdrawAmount())
                        .bankCode(account.getBankCode())      // ✅ DB값 사용
                        .accountNumber(account.getAccountNumber()) // ✅ DB값 사용
                        .status(WithdrawStatus.APPLY)
                        .build()
        );

        // ✅ 4. 응답 반환
        return WithdrawResponseDto.builder()
                .requestId(req.getId())
                .user_id(user.getId())
                .withdrawAmount(req.getWithdrawAmount())
                .status(req.getStatus())
                .requestDate(req.getRequestDate().format(DateTimeFormatter.ISO_DATE_TIME))
                .message("현금화 요청이 정상적으로 접수되었습니다.")
                .build();
    }
}
