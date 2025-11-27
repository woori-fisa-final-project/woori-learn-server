package dev.woori.wooriLearn.domain.edubankapiAccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiAccountDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransactionHistoryDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiAccountService;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdubankapiAccountServiceTest {

        @InjectMocks
        private EdubankapiAccountService service;

        @Mock
        private EdubankapiAccountRepository accountRepository;

        @Mock
        private EdubankapiTransactionHistoryRepository historyRepository;

        @Mock
        private UserRepository userRepository;

        private static final String TEST_USERNAME = "testuser";

        @BeforeEach
        void init() {
                MockitoAnnotations.openMocks(this);
        }

        /*
         * ----------------------------------------------------------
         * 1. 계좌 목록 조회 테스트 (JWT 인증 기반)
         * ----------------------------------------------------------
         */
        @Test
        void testGetAccountByUsername_Success() {
                // Mock User 객체 생성
                dev.woori.wooriLearn.domain.user.entity.Users mockUser = dev.woori.wooriLearn.domain.user.entity.Users
                                .builder()
                                .id(1L)
                                .userId(TEST_USERNAME)
                                .nickname("테스트유저")
                                .points(0)
                                .build();

                EducationalAccount acc1 = EducationalAccount.builder()
                                .id(1L)
                                .accountNumber("1111")
                                .balance(5000)
                                .accountName("계좌1")
                                .accountType(dev.woori.wooriLearn.domain.edubankapi.entity.AccountType.CHECKING)
                                .user(mockUser)
                                .build();
                EducationalAccount acc2 = EducationalAccount.builder()
                                .id(2L)
                                .accountNumber("2222")
                                .balance(7000)
                                .accountName("계좌2")
                                .accountType(dev.woori.wooriLearn.domain.edubankapi.entity.AccountType.SAVINGS)
                                .user(mockUser)
                                .build();

                // UserRepository mock 설정
                when(userRepository.findByUserId(TEST_USERNAME))
                                .thenReturn(Optional.of(mockUser));
                when(accountRepository.findByUser_Id(1L))
                                .thenReturn(List.of(acc1, acc2));

                // when
                List<EdubankapiAccountDto> list = service.getAccountByUsername(TEST_USERNAME);

                // then
                assertEquals(2, list.size());
                assertEquals("1111", list.get(0).accountNumber());
                assertEquals(TEST_USERNAME, list.get(0).userId());
                assertEquals("CHECKING", list.get(0).accountType()); // 계좌 타입 검증
                assertEquals("SAVINGS", list.get(1).accountType()); // 계좌 타입 검증
                verify(userRepository, times(1)).findByUserId(TEST_USERNAME);
                verify(accountRepository, times(1)).findByUser_Id(1L);
        }

        /*
         * ----------------------------------------------------------
         * 2. 계좌 목록 조회 실패 - 존재하지 않는 사용자
         * ----------------------------------------------------------
         */
        @Test
        void testGetAccountByUsername_UserNotFound() {
                // Mock 설정: 사용자를 찾을 수 없음
                when(userRepository.findByUserId("nonexistent"))
                                .thenReturn(Optional.empty());

                // when & then
                CommonException exception = assertThrows(CommonException.class,
                                () -> service.getAccountByUsername("nonexistent"));

                assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
                verify(userRepository, times(1)).findByUserId("nonexistent");
                verify(accountRepository, never()).findByUser_Id(any());
        }

        /*
         * ----------------------------------------------------------
         * 3. 거래내역 조회 - 기본 1개월 조회 테스트
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_Default1Month() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                TransactionHistory h1 = createHistory(1L, 1000); // 입금
                TransactionHistory h2 = createHistory(1L, -2000); // 출금

                when(historyRepository.findTransactionsByAccountIdAndDateRange(any(), any(), any()))
                                .thenReturn(List.of(h1, h2));

                List<EdubankapiTransactionHistoryDto> result = service.getTransactionList(
                                TEST_USERNAME, // username
                                1L, // accountId
                                null, // period (null → default 1M)
                                null, // startDate
                                null, // endDate
                                "ALL");

                assertEquals(2, result.size());
                verify(accountRepository, times(1)).existsByIdAndUser_UserId(1L, TEST_USERNAME);
                verify(historyRepository, times(1))
                                .findTransactionsByAccountIdAndDateRange(any(), any(), any());
        }

        /*
         * ----------------------------------------------------------
         * 4. 기간 직접 입력(startDate, endDate)
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_DateRangeDirect() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                LocalDate start = LocalDate.now().minusDays(5);
                LocalDate end = LocalDate.now();

                when(historyRepository.findTransactionsByAccountIdAndDateRange(any(), any(), any()))
                                .thenReturn(List.of(createHistory(1L, 1000)));

                List<EdubankapiTransactionHistoryDto> result = service.getTransactionList(
                                TEST_USERNAME, 1L, null, start, end, "ALL");

                assertEquals(1, result.size());

                // start, end 값이 repository 호출에 반영됐는지 검증
                ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
                ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

                verify(historyRepository).findTransactionsByAccountIdAndDateRange(
                                eq(1L),
                                startCaptor.capture(),
                                endCaptor.capture());

                assertEquals(start.atStartOfDay(), startCaptor.getValue());
                assertEquals(end.atTime(23, 59, 59), endCaptor.getValue());
        }

        /*
         * ----------------------------------------------------------
         * 5. Period 지정 (3개월)
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_Period3M() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                when(historyRepository.findTransactionsByAccountIdAndDateRange(any(), any(), any()))
                                .thenReturn(List.of(createHistory(1L, 500)));

                service.getTransactionList(TEST_USERNAME, 1L, "3M", null, null, "ALL");

                verify(historyRepository, times(1))
                                .findTransactionsByAccountIdAndDateRange(any(), any(), any());
        }

        /*
         * ----------------------------------------------------------
         * 6. 거래유형 필터링 (DEPOSIT)
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_FilterDeposit() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                TransactionHistory dep = createHistory(1L, 2000); // 입금
                TransactionHistory wit = createHistory(1L, -2000); // 출금

                when(historyRepository.findTransactionsByAccountIdAndDateRange(any(), any(), any()))
                                .thenReturn(List.of(dep, wit));

                List<EdubankapiTransactionHistoryDto> list = service.getTransactionList(
                                TEST_USERNAME, 1L, "1M", null, null, "DEPOSIT");

                assertEquals(1, list.size());
                assertTrue(list.get(0).amount() > 0);
        }

        /*
         * ----------------------------------------------------------
         * 7. 거래유형 필터링 (WITHDRAW)
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_FilterWithdraw() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                TransactionHistory dep = createHistory(1L, 2000);
                TransactionHistory wit = createHistory(1L, -2000);

                when(historyRepository.findTransactionsByAccountIdAndDateRange(any(), any(), any()))
                                .thenReturn(List.of(dep, wit));

                List<EdubankapiTransactionHistoryDto> list = service.getTransactionList(
                                TEST_USERNAME, 1L, "1M", null, null, "WITHDRAW");

                assertEquals(1, list.size());
                assertTrue(list.get(0).amount() < 0);
        }

        /*
         * ----------------------------------------------------------
         * 8. 기간 타입이 잘못된 경우 예외
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_InvalidPeriod_Throws() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                assertThrows(IllegalArgumentException.class,
                                () -> service.getTransactionList(TEST_USERNAME, 1L, "ABC", null, null, "ALL"));
        }

        /*
         * ----------------------------------------------------------
         * 9. 30건 초과 → 30건만 리턴되는지 테스트
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_Limit30() {
                // 소유권 검증 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(true);

                List<TransactionHistory> many = new ArrayList<>();
                for (int i = 0; i < 50; i++) {
                        many.add(createHistory((long) i, i));
                }

                when(historyRepository.findTransactionsByAccountIdAndDateRange(any(), any(), any()))
                                .thenReturn(many);

                List<EdubankapiTransactionHistoryDto> result = service.getTransactionList(
                                TEST_USERNAME, 1L, "1M", null, null, "ALL");

                assertEquals(30, result.size());
        }

        /*
         * ----------------------------------------------------------
         * 10. 소유권 검증 실패 - 타인 계좌 접근 시도
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_Unauthorized_Forbidden() {
                // 소유권 검증 실패 mock
                when(accountRepository.existsByIdAndUser_UserId(1L, TEST_USERNAME))
                                .thenReturn(false);

                CommonException exception = assertThrows(CommonException.class,
                                () -> service.getTransactionList(TEST_USERNAME, 1L, "1M", null, null, "ALL"));

                assertEquals("해당 계좌에 대한 접근 권한이 없습니다.", exception.getMessage());
                verify(accountRepository, times(1)).existsByIdAndUser_UserId(1L, TEST_USERNAME);
                verify(historyRepository, never()).findTransactionsByAccountIdAndDateRange(any(), any(), any());
        }

        /*
         * ----------------------------------------------------------
         * 11. accountId가 null인 경우 예외 발생
         * ----------------------------------------------------------
         */
        @Test
        void testGetTransactions_NullAccountId_Throws() {
                CommonException exception = assertThrows(CommonException.class,
                                () -> service.getTransactionList(TEST_USERNAME, null, "1M", null, null, "ALL"));

                assertEquals("계좌 ID는 필수입니다.", exception.getMessage());
                verify(accountRepository, never()).existsByIdAndUser_UserId(any(), any());
                verify(historyRepository, never()).findTransactionsByAccountIdAndDateRange(any(), any(), any());
        }

        /*
         * ----------------------------------------------------------
         * Helper: 테스트용 거래내역 생성
         * ----------------------------------------------------------
         */
        private TransactionHistory createHistory(Long id, int amount) {
                return TransactionHistory.builder()
                                .id(id)
                                .account(null)
                                .transactionDate(LocalDateTime.now())
                                .amount(amount)
                                .counterpartyName("홍길동")
                                .displayName("메모")
                                .description("테스트")
                                .build();
        }
}
