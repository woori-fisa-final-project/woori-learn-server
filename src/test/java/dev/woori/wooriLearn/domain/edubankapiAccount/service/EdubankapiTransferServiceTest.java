package dev.woori.wooriLearn.domain.edubankapiAccount.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiTransactionHistoryRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiTransferService;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.edubankapi.entity.TransactionHistory;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdubankapiTransferServiceTest {

    @InjectMocks
    private EdubankapiTransferService service;

    @Mock
    private EdubankapiAccountRepository accountRepository;

    @Mock
    private EdubankapiTransactionHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder encoder = new BCryptPasswordEncoder();

    private EducationalAccount from;
    private EducationalAccount to;
    private Users user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = Users.builder()
                .id(1L)
                .userId("user1")
                .nickname("u")
                .points(0)
                .build();

        when(userRepository.findByUserId("user1")).thenReturn(Optional.of(user));

        from = EducationalAccount.builder()
                .id(1L)
                .accountNumber("1122334455")
                .balance(5000)
                .accountPassword(encoder.encode("1111"))
                .accountName("출금계좌")
                .user(user)
                .build();

        to = EducationalAccount.builder()
                .id(2L)
                .accountNumber("5544332211")
                .balance(2000)
                .accountPassword(encoder.encode("1111"))
                .accountName("입금계좌")
                .user(user)
                .build();
    }

    @Test
    void 정상_이체() {
        when(accountRepository.findByAccountNumber("1122334455")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("5544332211")).thenReturn(Optional.of(to));

        EdubankapiTransferRequestDto req = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("5544332211")
                .amount(1000)
                .accountPassword("1111")
                .displayName("메모")
                .build();

        service.transfer("user1", req);

        assertEquals(4000, from.getBalance());
        assertEquals(3000, to.getBalance());
        verify(historyRepository, times(2)).save(any(TransactionHistory.class));
    }

    @Test
    void 비밀번호_불일치() {
        when(accountRepository.findByAccountNumber(any())).thenReturn(Optional.of(from));

        EdubankapiTransferRequestDto req = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("5544332211")
                .amount(1000)
                .accountPassword("9999")
                .displayName("메모")
                .build();

        assertThrows(CommonException.class, () -> service.transfer("user1", req));
    }

    @Test
    void 잔액부족() {
        from = EducationalAccount.builder()
                .accountNumber("1122334455")
                .balance(500)
                .accountPassword(encoder.encode("1111"))
                .accountName("출금계좌")
                .user(user)
                .build();

        when(accountRepository.findByAccountNumber("1122334455")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("5544332211")).thenReturn(Optional.of(to));

        EdubankapiTransferRequestDto req = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("5544332211")
                .amount(1000)
                .accountPassword("1111")
                .displayName("메모")
                .build();

        assertThrows(CommonException.class, () -> service.transfer("user1", req));
    }

    @Test
    void 금액_0이하일수() {
        EdubankapiTransferRequestDto req = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("5544332211")
                .amount(0)
                .accountPassword("1111")
                .displayName("메모")
                .build();

        when(accountRepository.findByAccountNumber("1122334455")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("5544332211")).thenReturn(Optional.of(to));

        assertThrows(CommonException.class, () -> service.transfer("user1", req));
    }

    @Test
    void 동일계좌() {
        EdubankapiTransferRequestDto req = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("1122334455")
                .toAccountNumber("1122334455")
                .amount(1000)
                .accountPassword("1111")
                .displayName("메모")
                .build();

        when(accountRepository.findByAccountNumber("1122334455")).thenReturn(Optional.of(from));

        assertThrows(CommonException.class, () -> service.transfer("user1", req));
    }

    @Test
    void reverse_locking_order_case() {
        from = EducationalAccount.builder()
                .id(1L)
                .accountNumber("9999999999")   // 락기준변경
                .balance(5000)
                .accountPassword(encoder.encode("1111"))
                .accountName("출금계좌")
                .user(user)
                .build();

        to = EducationalAccount.builder()
                .id(2L)
                .accountNumber("1111111111")   // 변경
                .balance(2000)
                .accountPassword(encoder.encode("1111"))
                .accountName("입금계좌")
                .user(user)
                .build();

        when(accountRepository.findByAccountNumber("9999999999")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("1111111111")).thenReturn(Optional.of(to));

        EdubankapiTransferRequestDto req = EdubankapiTransferRequestDto.builder()
                .fromAccountNumber("9999999999")
                .toAccountNumber("1111111111")
                .amount(500)
                .accountPassword("1111")
                .displayName("메모")
                .build();

        service.transfer("user1", req);

        assertEquals(4500, from.getBalance());
        assertEquals(2500, to.getBalance());
    }

    @Test
    void withdraw_fail() {
        EducationalAccount acc = EducationalAccount.builder()
                .balance(100)
                .build();

        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(0));
    }

    @Test
    void deposit_fail() {
        EducationalAccount acc = EducationalAccount.builder()
                .balance(100)
                .build();

        assertThrows(IllegalArgumentException.class, () -> acc.deposit(0));
    }
}
