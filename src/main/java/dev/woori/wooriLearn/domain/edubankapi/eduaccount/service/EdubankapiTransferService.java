package dev.woori.wooriLearn.domain.edubankapi.eduaccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferResponseDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdubankapiTransferService {
    private final EdubankapiAccountRepository accountRepository;
    private final EdubankapiTransactionHistoryRepository transactionHistoryRepository;

    /**
     *  계좌이체
     */
    @Transactional
    public EdubankapiTransferResponseDto transfer(EdubankapiTransferRequestDto request) {

        // 1. 출금 계좌 조회
        EducationalAccount fromAccount = accountRepository
                .findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 계좌를 찾을 수 없습니다."));

        // 2️. 입금 계좌 조회
        EducationalAccount toAccount = accountRepository
                .findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "입금 계좌를 찾을 수 없습니다."));

        // 3️. 비밀번호 검증
        if (!fromAccount.getAccountPassword().equals(request.accountPassword())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "계좌 비밀번호가 일치하지 않습니다.");
        }

        // 4. 잔액 검증
        if (fromAccount.getBalance() < request.amount()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "잔액이 부족합니다.");
        }

        // 5️. 자기 자신 송금 방지
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "같은 계좌로는 이체할 수 없습니다.");
        }

        // 6. 금액 차감 / 증액
        fromAccount.setBalance(fromAccount.getBalance() - request.amount());
        toAccount.setBalance(toAccount.getBalance() + request.amount());

        // 거래내역 생성
        TransactionHistory history = TransactionHistory.builder()
                .account(fromAccount)
                .transactionDate(LocalDateTime.now())
                .counterpartyName(request.counterpartyName())
                .amount(-request.amount())
                .description("계좌이체")
                .build();

        transactionHistoryRepository.save(history);

        // 8️. 응답 DTO 생성
        return EdubankapiTransferResponseDto.builder()
                .transactionId("TX-" + UUID.randomUUID().toString().substring(0, 8))
                .transactionDate(LocalDateTime.now())
                .counterpartyName(request.counterpartyName())
                .amount(request.amount())
                .balance(fromAccount.getBalance())
                .message("이체가 완료되었습니다.")
                .build();

    }
}
