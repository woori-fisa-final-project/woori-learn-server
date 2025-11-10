package dev.woori.wooriLearn.domain.edubackapi.autopayment.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentCreateRequest;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.repository.AutoPaymentRepository;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.service.AutoPaymentService;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("자동이체 Service 테스트")
class AutoPaymentServiceTest {

    @Mock
    private AutoPaymentRepository autoPaymentRepository;

    @Mock
    private EdubankapiAccountRepository edubankapiAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutoPaymentService autoPaymentService;

    private EducationalAccount mockAccount;
    private AutoPaymentCreateRequest validRequest;
    private AutoPayment mockAutoPayment;

    @BeforeEach
    void setUp() {
        Users mockUser = Users.builder()
                .id(1L)
                .userId("testuser")
                .build();

        mockAccount = EducationalAccount.builder()
                .id(1L)
                .accountNumber("1002-123-456789")
                .accountPassword("$2a$10$encodedPassword")
                .accountName("테스트계좌")
                .balance(1000000)
                .user(mockUser)
                .build();

        validRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        mockAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(50000)
                .counterpartyName("홍길동")
                .displayName("월세")
                .transferCycle(1)
                .designatedDate(5)
                .startDate(LocalDate.of(2025, 1, 1))
                .expirationDate(LocalDate.of(2025, 12, 31))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("자동이체 등록 성공")
    void createAutoPayment_Success() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(mockAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(validRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amount()).isEqualTo(50000);
        assertThat(response.processingStatus()).isEqualTo("ACTIVE");  // ✅ String

        verify(edubankapiAccountRepository).findById(1L);
        verify(passwordEncoder).matches("1234", mockAccount.getAccountPassword());
        verify(autoPaymentRepository).save(any(AutoPayment.class));
    }

    @Test
    @DisplayName("자동이체 등록 실패 - 계좌 없음 (ENTITY_NOT_FOUND)")
    void createAutoPayment_Fail_AccountNotFound() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> autoPaymentService.createAutoPayment(validRequest))
                .isInstanceOf(CommonException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

        assertThatThrownBy(() -> autoPaymentService.createAutoPayment(validRequest))
                .hasMessageContaining("교육용 계좌를 찾을 수 없습니다");

        verify(edubankapiAccountRepository, times(2)).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(autoPaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("자동이체 등록 실패 - 비밀번호 불일치 (UNAUTHORIZED)")
    void createAutoPayment_Fail_WrongPassword() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> autoPaymentService.createAutoPayment(validRequest))
                .isInstanceOf(CommonException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThatThrownBy(() -> autoPaymentService.createAutoPayment(validRequest))
                .hasMessageContaining("계좌 비밀번호가 일치하지 않습니다");

        verify(edubankapiAccountRepository, times(2)).findById(1L);
        verify(passwordEncoder, times(2)).matches("1234", mockAccount.getAccountPassword());
        verify(autoPaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("자동이체 목록 조회 성공 - 전체")
    void getAutoPaymentList_All_Success() {
        // given
        given(autoPaymentRepository.findByEducationalAccountId(anyLong()))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "ALL");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).processingStatus()).isEqualTo("ACTIVE");

        verify(autoPaymentRepository).findByEducationalAccountId(1L);
    }

    @Test
    @DisplayName("자동이체 목록 조회 성공 - ACTIVE만")
    void getAutoPaymentList_ActiveOnly_Success() {
        // given
        given(autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                anyLong(), any(AutoPaymentStatus.class)))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "ACTIVE");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).processingStatus()).isEqualTo("ACTIVE");

        verify(autoPaymentRepository).findByEducationalAccountIdAndProcessingStatus(
                1L, AutoPaymentStatus.ACTIVE);
    }

    @Test
    @DisplayName("자동이체 상세 조회 성공")
    void getAutoPaymentDetail_Success() {
        // given
        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAutoPayment));

        // when
        AutoPaymentResponse response = autoPaymentService.getAutoPaymentDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.displayName()).isEqualTo("월세");
        assertThat(response.processingStatus()).isEqualTo("ACTIVE");

        verify(autoPaymentRepository).findById(1L);
    }

    @Test
    @DisplayName("자동이체 상세 조회 실패 - 존재하지 않음 (ENTITY_NOT_FOUND)")
    void getAutoPaymentDetail_Fail_NotFound() {
        // given
        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> autoPaymentService.getAutoPaymentDetail(1L))
                .isInstanceOf(CommonException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

        assertThatThrownBy(() -> autoPaymentService.getAutoPaymentDetail(1L))
                .hasMessageContaining("자동이체 정보를 찾을 수 없습니다");

        verify(autoPaymentRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("상태 변환 실패 - 잘못된 상태값 (INVALID_REQUEST)")
    void resolveStatus_Invalid() {
        // when & then
        assertThatThrownBy(() -> autoPaymentService.getAutoPaymentList(1L, "INVALID_STATUS"))
                .isInstanceOf(CommonException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> autoPaymentService.getAutoPaymentList(1L, "INVALID_STATUS"))
                .hasMessageContaining("유효하지 않은 상태 값입니다");
    }
}