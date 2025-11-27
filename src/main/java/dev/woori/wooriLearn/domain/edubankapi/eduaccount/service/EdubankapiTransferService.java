package dev.woori.wooriLearn.domain.edubankapi.eduaccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferResponseDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 계좌이체 (보안 강화: 출금 계좌 소유권 검증 추가)
     * <p>
     * - 트랜잭션 경계 내에서 실행 (@Transactional)
     * - JWT 토큰 기반 출금 계좌 소유권 검증
     * - 비관적 락으로 동시성 제어
     * - 교착 방지를 위해 계좌번호 기준 정렬 후 락 획득
     * - 비밀번호/잔액/자기계좌 검증 수행
     * - 잔액 변경 및 거래내역 저장을 원자적으로 처리
     *
     * @param username JWT 토큰에서 추출한 사용자 ID
     * @param request  계좌이체 요청 정보
     */
    @Transactional
    public EdubankapiTransferResponseDto transfer(String username, EdubankapiTransferRequestDto request) {

        log.info("[계좌이체 요청] username={} from={} to={} amount={} displayName={}",
                username, request.fromAccountNumber(), request.toAccountNumber(), request.amount(),
                request.displayName());

        // 0. 사용자 조회
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 1. 교착 방지 - 락 순서 고정
        EducationalAccount fromAccount;
        EducationalAccount toAccount;

        if (request.fromAccountNumber().compareTo(request.toAccountNumber()) < 0) {
            fromAccount = findAccountByNumberOrThrow(request.fromAccountNumber(), "출금 계좌를 찾을 수 없습니다.");
            toAccount = findAccountByNumberOrThrow(request.toAccountNumber(), "입금 계좌를 찾을 수 없습니다.");
        } else {
            toAccount = findAccountByNumberOrThrow(request.toAccountNumber(), "입금 계좌를 찾을 수 없습니다.");
            fromAccount = findAccountByNumberOrThrow(request.fromAccountNumber(), "출금 계좌를 찾을 수 없습니다.");
        }

        // 2️. 검증 로직 (출금 계좌 소유권 검증 추가)
        validateTransfer(user, request, fromAccount, toAccount);

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
                now);

        TransactionHistory depositHistory = createHistory(
                toAccount,
                request.amount(),
                request.counterpartyName(),
                request.displayName(),
                "계좌이체(입금)",
                now);

        transactionHistoryRepository.save(withdrawHistory);
        transactionHistoryRepository.save(depositHistory);

        // 5️. 응답 DTO 구성 (하이픈 포함된 계좌번호 응답)
        EdubankapiTransferResponseDto response = EdubankapiTransferResponseDto.of(
                "TX-" + UUID.randomUUID().toString().substring(0, 8),
                now,
                request.counterpartyName(),
                request.amount(),
                fromAccount.getBalance(),
                "이체가 완료되었습니다.",
                fromAccount.getAccountNumber() // DB 원본 계좌번호 (하이픈 없음)
        );

        log.info("[계좌이체 완료] from={} to={} amount={} fromBalanceAfter={}",
                fromAccount.getAccountNumber(), toAccount.getAccountNumber(), request.amount(),
                fromAccount.getBalance());

        return response;

    }

    /**
     * 계좌번호로 계좌 조회 헬퍼 메서드
     *
     * @param accountNumber 계좌번호
     * @param errorMessage 계좌를 찾을 수 없을 때 예외 메시지
     * @return 조회된 계좌 (비관적 락 적용됨)
     */
    private EducationalAccount findAccountByNumberOrThrow(String accountNumber, String errorMessage) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, errorMessage));
    }

    /**
     * 거래내역 생성 헬퍼 메서드
     */
    private TransactionHistory createHistory(
            EducationalAccount account,
            int amount,
            String counterparty,
            String display,
            String description,
            LocalDateTime date) {
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
     * 계좌이체 검증 로직 (보안 강화: 출금 계좌 소유권 검증 추가)
     */
    private void validateTransfer(Users user,
                                  EdubankapiTransferRequestDto request,
                                  EducationalAccount fromAccount,
                                  EducationalAccount toAccount) {

        // 🔒 출금 계좌 소유권 검증 (가장 먼저!)
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인 소유의 계좌에서만 출금할 수 있습니다.");
        }

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
