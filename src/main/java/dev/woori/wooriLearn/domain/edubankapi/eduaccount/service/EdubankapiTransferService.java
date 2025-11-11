package dev.woori.wooriLearn.domain.edubankapi.eduaccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferResponseDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdubankapiTransferService {

    private final EdubankapiAccountRepository accountRepository;
    private final EdubankapiTransactionHistoryRepository transactionHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     *  계좌이체
     *
     *  - 트랜잭션 경계 내에서 실행 (@Transactional)
     *  - 비관적 락으로 동시성 제어
     *  - 교착 방지를 위해 계좌번호 기준 정렬 후 락 획득
     *  - 비밀번호/잔액/자기계좌 검증 수행
     *  - 잔액 변경 및 거래내역 저장을 원자적으로 처리
     */
    @Transactional
    public EdubankapiTransferResponseDto transfer(EdubankapiTransferRequestDto request) {

        log.info("[계좌이체 요청] from={} to={} amount={} displayName={}",
                request.fromAccountNumber(), request.toAccountNumber(), request.amount(), request.displayName());

        // 1. 교착 방지 - 락 순서 고정
        EducationalAccount fromAccount;
        EducationalAccount toAccount;

        if (request.fromAccountNumber().compareTo(request.toAccountNumber()) < 0) {
            fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 계좌를 찾을 수 없습니다."));
            toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "입금 계좌를 찾을 수 없습니다."));
        } else {
            toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "입금 계좌를 찾을 수 없습니다."));
            fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 계좌를 찾을 수 없습니다."));
        }

        // 2️. 검증 로직
        validateTransfer(request, fromAccount, toAccount);

        // 3️. 잔액 변경 (도메인 메서드로 책임 위임)
        fromAccount.withdraw(request.amount());
        toAccount.deposit(request.amount());

        // 명시적 저장 (Dirty Checking 의존 대신 명확하게)
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 4️. 거래내역 생성
        LocalDateTime now = LocalDateTime.now();

        TransactionHistory withdrawHistory = createHistory(
                fromAccount,
                -request.amount(),
                request.counterpartyName(),
                request.displayName(),
                "계좌이체(출금)",
                now
        );

        TransactionHistory depositHistory = createHistory(
                toAccount,
                request.amount(),
                request.counterpartyName(),
                request.displayName(),
                "계좌이체(입금)",
                now
        );

        transactionHistoryRepository.save(withdrawHistory);
        transactionHistoryRepository.save(depositHistory);

        // 5️. 응답 DTO 구성
        EdubankapiTransferResponseDto response = EdubankapiTransferResponseDto.builder()
                .transactionId("TX-" + UUID.randomUUID().toString().substring(0, 8))
                .transactionDate(now)
                .counterpartyName(request.counterpartyName())
                .amount(request.amount())
                .balance(fromAccount.getBalance())
                .message("이체가 완료되었습니다.")
                .build();

        log.info("[계좌이체 완료] from={} to={} amount={} fromBalanceAfter={}",
                fromAccount.getAccountNumber(), toAccount.getAccountNumber(), request.amount(), fromAccount.getBalance());

        return response;
    }

    /**
     *  거래내역 생성 헬퍼 메서드
     */
    private TransactionHistory createHistory(
            EducationalAccount account,
            int amount,
            String counterparty,
            String display,
            String description,
            LocalDateTime date
    ) {
        return TransactionHistory.builder()
                .account(account)
                .transactionDate(date)
                .counterpartyName(counterparty)
                .displayName(display)
                .amount(amount)
                .description(description)
                .build();
    }

    /**
     *  계좌이체 검증 로직
     */
    private void validateTransfer(EdubankapiTransferRequestDto request,
                                  EducationalAccount fromAccount,
                                  EducationalAccount toAccount) {

        // 동일 계좌 송금 금지
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "같은 계좌로는 이체할 수 없습니다.");
        }

        // 비밀번호 불일치
        if (!passwordEncoder.matches(request.accountPassword(), fromAccount.getAccountPassword())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "계좌 비밀번호가 일치하지 않습니다.");
        }

        // 잔액 부족
        if (fromAccount.getBalance() < request.amount()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "잔액이 부족합니다.");
        }

        // 금액 유효성
        if (request.amount() <= 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "이체 금액은 0보다 커야 합니다.");
        }
    }
}
