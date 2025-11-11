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

        // 1. 출금 계좌 조회: 요청 바디의 fromAccountNumber로 출금계좌를 찾고, 없으면 404 성격의 예외 발생
        EducationalAccount fromAccount = accountRepository
                .findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 계좌를 찾을 수 없습니다."));

        // 2. 입금 계좌 조회: 요청 바디의 toAccountNumber로 입금계좌를 찾고, 없으면 예외
        EducationalAccount toAccount = accountRepository
                .findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "입금 계좌를 찾을 수 없습니다."));

        // 3. 비밀번호 검증: 출금계좌의 계좌비밀번호와 요청 비밀번호가 다르면 인증 실패 예외
        if (!fromAccount.getAccountPassword().equals(request.accountPassword())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "계좌 비밀번호가 일치하지 않습니다.");
        }

        // 4. 잔액 검증: 출금계좌 잔액이 이체금액보다 작으면 잔액 부족 예외
        if (fromAccount.getBalance() < request.amount()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "잔액이 부족합니다.");
        }

        // 5. 자기계좌 송금 방지: 출금/입금 계좌번호가 같으면 요청 자체를 거부
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "같은 계좌로는 이체할 수 없습니다.");
        }

        // 6. 금액 반영: 출금계좌 잔액 차감, 입금계좌 잔액 증가
        fromAccount.setBalance(fromAccount.getBalance() - request.amount());
        toAccount.setBalance(toAccount.getBalance() + request.amount());

        // 7. 거래내역 생성(출금 측): 출금계좌 기준으로 금액은 음수 기록, 간단한 설명 부여
        TransactionHistory history = TransactionHistory.builder()
                .account(fromAccount)                               // 어떤 계좌의 거래인지(출금 측)
                .transactionDate(LocalDateTime.now())               // 거래 발생 시각(서버 기준)
                .counterpartyName(request.counterpartyName())       // 상대 이름(수취인)
                .displayName(request.displayName())                 // 받는 사람 표시명
                .amount(-request.amount())                          // 출금은 음수로 기록
                .description("계좌이체")                             // 거래 유형 설명
                .build();

        // 출금 거래내역 저장
        transactionHistoryRepository.save(history);

        // 8. 응답 DTO 구성: 트랜잭션 식별자/시각/상대이름/이체금액/출금 후 잔액/메시지 채워 반환
        return EdubankapiTransferResponseDto.builder()
                .transactionId("TX-" + UUID.randomUUID().toString().substring(0, 8)) // 간단한 랜덤 트랜잭션 ID
                .transactionDate(LocalDateTime.now())           // 응답 시각(표시용)
                .counterpartyName(request.counterpartyName())   // 상대 이름(수취인)
                .amount(request.amount())                       // 요청된 이체금액(양수)
                .balance(fromAccount.getBalance())              // 이체 후 출금계좌 잔액
                .message("이체가 완료되었습니다.")                 // 처리 결과 메시지
                .build();

    }
}
