package dev.woori.wooriLearn.domain.edubankapi.eduaccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiAccountDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransactionHistoryDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.PasswordCheckRequest;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.validation.PeriodType;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.validation.TransactionType;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     *      사용자 username을 통해 계좌 목록 조회 (JWT 인증용)
     *
     *         - JWT 토큰에서 추출한 username으로 사용자를 조회
     *         - 해당 사용자의 모든 교육용 계좌를 조회
     *         - Entity를 DTO로 변환하여 Controller에 전달
     *
     *         @param username : 사용자 ID (JWT 토큰에서 추출)
     *         @return 계좌 목록 : <List<AccountDto>>
     */
    public List<EdubankapiAccountDto> getAccountByUsername(String username) {
        // username으로 사용자 조회
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // Repository 호출을 통해 educationl_account 테이블에 user_id가 일치하는 계좌 엔티티 목록 조회
        List<EducationalAccount> accounts = edubankapiAccountRepository.findByUser_Id(user.getId());

        return accounts.stream()
                .map(EdubankapiAccountDto::from)
                .collect(Collectors.toList());
    }

    /**
     *   거래내역 목록 조회 (보안 강화: 계좌 소유권 검증 추가)
     *
     *   @param username JWT 토큰에서 추출한 사용자 ID
     *   @param accountId 조회할 계좌 ID
     *   @param period 조회 기간
     *   @param startDate 시작일
     *   @param endDate 종료일
     *   @param type 거래 구분
     */
    public List<EdubankapiTransactionHistoryDto> getTransactionList(
            String username,
            Long accountId,
            String period,
            LocalDate startDate,
            LocalDate endDate,
            String type
    ) {
        // 0. 계좌 소유권 검증
        validateAccountOwnership(username, accountId);

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

    /**
     * 계좌 소유권 검증 헬퍼 메서드
     *
     * @param username 사용자 ID (JWT에서 추출)
     * @param accountId 계좌 ID
     * @throws CommonException 계좌가 존재하지 않거나 소유자가 아닌 경우
     */
    private void validateAccountOwnership(String username, Long accountId) {
        // accountId null 체크 (방어적 프로그래밍)
        if (accountId == null) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "계좌 ID는 필수입니다.");
        }

        if (!edubankapiAccountRepository.existsByIdAndUser_UserId(accountId, username)) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "해당 계좌를 찾을 수 없습니다.");
        }
    }

    /**
     * 계좌 비밀번호 일치 여부 확인 (Scenario 5)
     */
    @Transactional(readOnly = true) // 락을 안 쓰므로 readOnly = true 권장 (실패 카운트 업데이트 로직이 없다면)
    public boolean checkPassword(String username, PasswordCheckRequest request) {

        EducationalAccount account = edubankapiAccountRepository.findByAccountNumberForRead(request.accountNumber())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "계좌를 찾을 수 없습니다."));

        // 2. 소유주 검증
        if (!account.getUser().getUserId().equals(username)) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인 계좌의 비밀번호만 확인할 수 있습니다.");
        }

        // 3. 비밀번호 매칭
        return passwordEncoder.matches(request.password(), account.getAccountPassword());

    }
}

