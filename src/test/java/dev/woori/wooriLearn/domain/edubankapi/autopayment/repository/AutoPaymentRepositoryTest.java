package dev.woori.wooriLearn.domain.edubankapi.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.user.entity.Role;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AutoPaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AutoPaymentRepository autoPaymentRepository;

    private Users testUser;
    private EducationalAccount educationalAccount;
    private AutoPayment activeAutoPayment;
    private AutoPayment cancelledAutoPayment;

    @BeforeEach
    void setUp() {
        // Users 엔티티를 먼저 생성하고 저장
        testUser = Users.builder()
                .userId("testuser")
                .password("encodedPassword")
                .nickname("테스트유저")
                .points(1000)
                .role(Role.ROLE_USER)
                .build();
        entityManager.persist(testUser);

        // EducationalAccount 생성 시 user를 설정
        educationalAccount = EducationalAccount.builder()
                .accountNumber("123456789")
                .accountPassword("encodedPassword")
                .accountName("김테스트")
                .balance(100000)
                .user(testUser)
                .build();
        entityManager.persist(educationalAccount);

        activeAutoPayment = AutoPayment.builder()
                .educationalAccount(educationalAccount)
                .depositNumber("1234567890")
                .depositBankCode("001")
                .amount(50000)
                .counterpartyName("김철수")
                .displayName("용돈")
                .transferCycle(1)
                .designatedDate(15)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusYears(1))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();
        entityManager.persist(activeAutoPayment);

        cancelledAutoPayment = AutoPayment.builder()
                .educationalAccount(educationalAccount)
                .depositNumber("9876543210")
                .depositBankCode("002")
                .amount(30000)
                .counterpartyName("이영희")
                .displayName("생활비")
                .transferCycle(1)
                .designatedDate(20)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusYears(1))
                .processingStatus(AutoPaymentStatus.CANCELLED)
                .build();
        entityManager.persist(cancelledAutoPayment);

        entityManager.flush();
    }

    @Test
    @DisplayName("교육용 계좌 ID로 자동이체 조회")
    void findByEducationalAccountId() {
        // when
        List<AutoPayment> results = autoPaymentRepository.findByEducationalAccountId(educationalAccount.getId());

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(AutoPayment::getEducationalAccount)
                .allMatch(account -> account.getId().equals(educationalAccount.getId()));
    }

    @Test
    @DisplayName("교육용 계좌 ID와 상태로 자동이체 조회 - ACTIVE")
    void findByEducationalAccountIdAndProcessingStatus_Active() {
        // when
        List<AutoPayment> results = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                educationalAccount.getId(), AutoPaymentStatus.ACTIVE);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProcessingStatus()).isEqualTo(AutoPaymentStatus.ACTIVE);
        assertThat(results.get(0).getId()).isEqualTo(activeAutoPayment.getId());
    }

    @Test
    @DisplayName("교육용 계좌 ID와 상태로 자동이체 조회 - CANCELLED")
    void findByEducationalAccountIdAndProcessingStatus_Cancelled() {
        // when
        List<AutoPayment> results = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                educationalAccount.getId(), AutoPaymentStatus.CANCELLED);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProcessingStatus()).isEqualTo(AutoPaymentStatus.CANCELLED);
        assertThat(results.get(0).getId()).isEqualTo(cancelledAutoPayment.getId());
    }

    @Test
    @DisplayName("존재하지 않는 교육용 계좌 ID로 조회시 빈 리스트 반환")
    void findByEducationalAccountId_NotExists() {
        // when
        List<AutoPayment> results = autoPaymentRepository.findByEducationalAccountId(999L);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("자동이체 ID로 상세 조회")
    void findById() {
        // when
        Optional<AutoPayment> result = autoPaymentRepository.findById(activeAutoPayment.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(activeAutoPayment.getId());
        assertThat(result.get().getEducationalAccount()).isNotNull();
        assertThat(result.get().getEducationalAccount().getId()).isEqualTo(educationalAccount.getId());
    }

    @Test
    @DisplayName("존재하지 않는 자동이체 ID로 조회시 빈 Optional 반환")
    void findById_NotExists() {
        // when
        Optional<AutoPayment> result = autoPaymentRepository.findById(999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("자동이체 저장")
    void save() {
        // given
        AutoPayment newAutoPayment = AutoPayment.builder()
                .educationalAccount(educationalAccount)
                .depositNumber("5555555555")
                .depositBankCode("003")
                .amount(20000)
                .counterpartyName("박민수")
                .displayName("통신비")
                .transferCycle(1)
                .designatedDate(25)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusYears(1))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        // when
        AutoPayment savedAutoPayment = autoPaymentRepository.save(newAutoPayment);

        // then
        assertThat(savedAutoPayment.getId()).isNotNull();
        assertThat(savedAutoPayment.getDepositNumber()).isEqualTo("5555555555");
        assertThat(savedAutoPayment.getEducationalAccount().getId()).isEqualTo(educationalAccount.getId());
    }

    @Test
    @DisplayName("EntityGraph가 적용되어 교육용 계좌가 즉시 로딩됨")
    void entityGraphLoading() {
        // when
        List<AutoPayment> results = autoPaymentRepository.findByEducationalAccountId(educationalAccount.getId());

        // then
        entityManager.clear(); // 영속성 컨텍스트 클리어

        // EntityGraph로 인해 교육용 계좌가 이미 로딩되어 있어야 함
        assertThat(results).hasSize(2);
        for (AutoPayment autoPayment : results) {
            assertThat(autoPayment.getEducationalAccount()).isNotNull();
            assertThat(autoPayment.getEducationalAccount().getAccountNumber()).isNotNull();
        }
    }

    @Test
    @DisplayName("자동이체 삭제")
    void delete() {
        // given
        Long autoPaymentId = activeAutoPayment.getId();

        // when
        autoPaymentRepository.delete(activeAutoPayment);
        entityManager.flush();

        // then
        Optional<AutoPayment> result = autoPaymentRepository.findById(autoPaymentId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("교육용 계좌별 자동이체 개수 확인")
    void countByEducationalAccount() {
        // when
        List<AutoPayment> results = autoPaymentRepository.findByEducationalAccountId(educationalAccount.getId());

        // then
        assertThat(results).hasSize(2);
    }
}