package dev.woori.wooriLearn.domain.edubankapi.eduaccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferResponseDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import org.springframework.transaction.annotation.Transactional;
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
     *
     *  - 트랜잭션 경계에서 실행되어야 함 (@Transactional)
     *  - 동시성 제어 : 출금/입금 계좌에 비관적 쓰기 락을 걸고 작업
     *  - 교착 방지 : 두 계좌번호를 비교하여 "항상 동일한 락 획득 순서"를 보장
     *  - 업무 검증 : 비밀번호 일치, 잔액 부족, 자기계좌 송금 금지 등
     *  - 원자성 : 예외 발생 시 트랜잭션 롤벡 -> 잔액/거래내역 모두 되돌림
     */
    @Transactional
    public EdubankapiTransferResponseDto transfer(EdubankapiTransferRequestDto request) {

        // 로깅 (혹시 모를 오류 추적용)
        log.info("[계좌이체 요청] from={} to={} amount={} displayName={}",
                request.fromAccountNumber(), request.toAccountNumber(), request.amount(), request.displayName());

        /**
         *      1. 교착 상태 방지를 위한 락 획득 순서 고정
         *      - 두 계좌번호를 문자열과 비교하여 항상 작은 번호 -> 큰 번호 순으로 락을 걸도록 강제
         *      - findByAccountNumber : 비관적 락응로 조회
         */

        EducationalAccount fromAccount;     // 출금 계좌
        EducationalAccount toAccount;       // 입금 계좌


        /*  compareTo() < 0 -> from < to 이면 from -> to 순서로 락걸리게
            compareTo : 두 객체(문자열)를 비교하고, 그 결과로 정수 값을 반환
                음수 : 메서드를 호출한 현재 객체가 인자로 전달된 다른 객체보다 사전순으로 앞서거나, 작다는 의미 (현재 객체 < 다른 객체)
                 0  : 두 객체 동등 (현재 객체 == 다른 객체)
                양수 : 메서드를 호출한 현재 객체가 인자로 전달된 다른 객체보다 사전순으로 뒤에 있거나 크다, (현재 객체 > 다른 객체)
         */
        if(request.fromAccountNumber().compareTo(request.toAccountNumber()) < 0 ) {
            fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 계좌를 찾을 수 없습니다."));
            toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "입금 계좌를 찾을 수 없습니다."));
        }
        else {
            // 반대 경우에는 to -> from 락
            toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "입금 계좌를 찾을 수 없습니다."));
            fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "출금 계좌를 찾을 수 없습니다."));
        }

        /**
         *      2. 검증 로직
         */
        // 자기계좌 송금 방지: 동일 계좌 간 이체는 비즈니스 상 금지
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "같은 계좌로는 이체할 수 없습니다.");
        }

        // 비밀번호 검증: 출금 계좌 비밀번호 불일치 시 인증 실패
        if (!fromAccount.getAccountPassword().equals(request.accountPassword())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "계좌 비밀번호가 일치하지 않습니다.");
        }

        // 잔액 검증: 출금 잔액이 이체 금액보다 작으면 거절
        if (fromAccount.getBalance() < request.amount()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "잔액이 부족합니다.");
        }

        /**
         *      3. 상태 변경
         *      - 출금 계좌 : - 잔액
         *      - 입금 계좌 - + 잔액
         *      - 같은 트랜잭션/락 범위 내에서 원자적으로 이루어짐
         */
        fromAccount.setBalance(fromAccount.getBalance() - request.amount());
        toAccount.setBalance(toAccount.getBalance() + request.amount());

        /**
         *      4. 거래내역 기록
         */
        //  출금 거래내역 (금액 음수)
        TransactionHistory withdrawHistory = TransactionHistory.builder()
                .account(fromAccount)                            // 거래 주체: 출금계좌
                .transactionDate(LocalDateTime.now())            // 거래 시각
                .counterpartyName(request.counterpartyName())    // 상대방 이름 (수취인)
                .displayName(request.displayName())              // 내통장표시
                .amount(-request.amount())                       // 출금은 음수
                .description("계좌이체(출금)")                      // 유형 설명
                .build();
        transactionHistoryRepository.save(withdrawHistory);

        //  입금 거래내역 (금액 양수) - 선택이지만 권장
        TransactionHistory depositHistory = TransactionHistory.builder()
                .account(toAccount)                              // 거래 주체: 입금계좌
                .transactionDate(withdrawHistory.getTransactionDate())  // 동일 시각으로 맞추면 조회/정산 편함
                .counterpartyName(request.counterpartyName())    // 보내는 이/표시명 정책은 서비스 정책에 맞게
                .displayName(request.displayName())
                .amount(request.amount())                        // 입금은 양수
                .description("계좌이체(입금)")
                .build();
        transactionHistoryRepository.save(depositHistory);

        /**
         *      5. 응답 구성
         *      - 트랜잭션 ID 여기서는 간단한 UUID 일부 사용
         *      - balance는 출금 계좌의 거래 후 잔액을 반환
         */
        EdubankapiTransferResponseDto response = EdubankapiTransferResponseDto.builder()
                .transactionId("TX-" + UUID.randomUUID().toString().substring(0, 8))
                .transactionDate(withdrawHistory.getTransactionDate())
                .counterpartyName(request.counterpartyName())
                .amount(request.amount())
                .balance(fromAccount.getBalance())
                .message("이체가 완료되었습니다.")
                .build();

        // 로깅 반환
        log.info("[계좌이체 완료] from={} to={} amount={} fromBalanceAfter={}",
                fromAccount.getAccountNumber(), toAccount.getAccountNumber(), request.amount(), fromAccount.getBalance());

        return response;
    }
}
