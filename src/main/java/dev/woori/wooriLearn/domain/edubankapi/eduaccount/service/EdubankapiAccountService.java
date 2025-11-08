package dev.woori.wooriLearn.domain.edubankapi.eduaccount.service;

import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiAccountDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransactionHistoryDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.validation.PeriodType;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.validation.TransactionType;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor    // 의존성 주입하기 위해서 선언 final 필드를 인자로 받는 생성자로 자동 생성
public class EdubankapiAccountService {

    // 의존성 주입 대상
    private final EdubankapiAccountRepository edubankapiAccountRepository;
    private final EdubankapiTransactionHistoryRepository edubankapiTransactionHistoryRepository;

    /*
        사용자 ID를 통해 계좌 목록 조회

        - Repository를 통해 DB에서 특정 사용자{userId}의 계좌 데이터를 조회
        - Entity를 DTO로 변환하여 Controller에 전달

        @param userId : 사용자 ID
        @return 계좌 목록 : <List<AccountDto>>
     */
    public List<EdubankapiAccountDto> getAccountByUserId(long userId) {

        // Repository 호출을 통해 educationl_account 테이블에 user_id가 일치하는 계좌 엔티티 목록 조회
        List<EducationalAccount> accounts = edubankapiAccountRepository.findByUserId(userId);

        return accounts.stream()
                .map(EdubankapiAccountDto::from)
                .collect(Collectors.toList());
    }

    /**
     *   거래내역 목록 조회
     */
    public List<EdubankapiTransactionHistoryDto> getTransactionList(
            Long accountId,
            String period,
            LocalDate startDate,
            LocalDate endDate,
            String type
    ) {
        // 1. Enum 변환 시 유효성 검사
        PeriodType periodType = null;
        if (period != null && !period.isBlank()) {
            periodType = PeriodType.from(period); // 잘못된 값이면 예외 발생
        }

        TransactionType transactionType = TransactionType.from(
                type != null ? type : "ALL" // null이면 기본 ALL
        );

        // 2. 조회 기간 계산
        LocalDateTime end = (endDate != null ? endDate : LocalDate.now()).atTime(23, 59, 59);
        LocalDateTime start;

        if (startDate != null) {
            start = startDate.atStartOfDay();
        } else if (periodType != null) {
            if (periodType == PeriodType.ONE_YEAR) start = end.minusYears(1);
            else start = end.minusMonths(periodType.getMonths());
        } else {
            start = end.minusMonths(1);
        }

        // 3. DB 조회
        List<TransactionHistory> histories =
                edubankapiTransactionHistoryRepository.findTransactionsByAccountIdAndDateRange(
                        accountId, start, end
                );

        // 4. 거래 유형 필터링
        return histories.stream()
                .filter(h -> switch (transactionType) {
                    case ALL -> true;
                    case DEPOSIT -> h.getAmount() > 0;
                    case WITHDRAW -> h.getAmount() < 0;
                })
                .limit(30)
                .map(EdubankapiTransactionHistoryDto::from)
                .collect(Collectors.toList());
    }
}
