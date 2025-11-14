package dev.woori.wooriLearn.domain.edubankapi.autopayment.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentCreateRequest;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.repository.AutoPaymentRepository;
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
                .authUser(AuthUsers.builder().userId("testuser").build())
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
                LocalDate.of(2025, 12, 31),  // ✅ null 아님
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
                .expirationDate(LocalDate.of(2025, 12, 31))  // ✅ null 아님
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("자동이체 등록 성공")
    void createAutoPayment_Success() {
        // given
        // validateAccountOwnership + findAndValidateAccount에서 총 2번 호출됨
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(mockAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(validRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amount()).isEqualTo(50000);
        assertThat(response.processingStatus()).isEqualTo("ACTIVE");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(response.expirationDate()).isEqualTo(LocalDate.of(2025, 12, 31));

        verify(edubankapiAccountRepository, times(2)).findById(1L);  // 2번 호출 검증
        verify(passwordEncoder).matches("1234", mockAccount.getAccountPassword());
        verify(autoPaymentRepository).save(any(AutoPayment.class));
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 시작일과 만료일이 같은 날")
    void createAutoPayment_Success_SameDate() {
        // given
        LocalDate sameDate = LocalDate.of(2025, 1, 1);
        AutoPaymentCreateRequest sameDateRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                sameDate, sameDate,  // 같은 날
                "1234"
        );

        AutoPayment sameDateAutoPayment = AutoPayment.builder()
                .id(2L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(50000)
                .counterpartyName("홍길동")
                .displayName("월세")
                .transferCycle(1)
                .designatedDate(5)
                .startDate(sameDate)
                .expirationDate(sameDate)
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(sameDateAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(sameDateRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.startDate()).isEqualTo(sameDate);
        assertThat(response.expirationDate()).isEqualTo(sameDate);

        verify(edubankapiAccountRepository, times(2)).findById(1L);
        verify(autoPaymentRepository).save(any(AutoPayment.class));
    }

    @Test
    @DisplayName("자동이체 등록 실패 - 계좌 없음 (ENTITY_NOT_FOUND)")
    void createAutoPayment_Fail_AccountNotFound() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.createAutoPayment(validRequest,"testuser"),
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
        assertThat(exception.getMessage()).contains("교육용 계좌를 찾을 수 없습니다");

        verify(edubankapiAccountRepository).findById(1L);
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

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.createAutoPayment(validRequest,"testuser"),
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).contains("계좌 비밀번호가 일치하지 않습니다");

        verify(edubankapiAccountRepository, times(2)).findById(1L);  // 2번 호출 검증
        verify(passwordEncoder).matches("1234", mockAccount.getAccountPassword());
        verify(autoPaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("자동이체 목록 조회 성공 - 전체")
    void getAutoPaymentList_All_Success() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountId(anyLong()))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "ALL","testuser");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).processingStatus()).isEqualTo("ACTIVE");

        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountId(1L);
    }

    @Test
    @DisplayName("자동이체 목록 조회 성공 - ACTIVE만")
    void getAutoPaymentList_ActiveOnly_Success() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                anyLong(), any(AutoPaymentStatus.class)))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "ACTIVE","testuser");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).processingStatus()).isEqualTo("ACTIVE");

        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountIdAndProcessingStatus(
                1L, AutoPaymentStatus.ACTIVE);
    }

    @Test
    @DisplayName("자동이체 상세 조회 성공")
    void getAutoPaymentDetail_Success() {
        // given
        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAutoPayment));
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));

        // when
        AutoPaymentResponse response = autoPaymentService.getAutoPaymentDetail(1L,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.displayName()).isEqualTo("월세");
        assertThat(response.processingStatus()).isEqualTo("ACTIVE");

        verify(autoPaymentRepository).findById(1L);
        verify(edubankapiAccountRepository).findById(1L);
    }

    @Test
    @DisplayName("자동이체 상세 조회 실패 - 존재하지 않음 (ENTITY_NOT_FOUND)")
    void getAutoPaymentDetail_Fail_NotFound() {
        // given
        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.getAutoPaymentDetail(1L,"testuser"),
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
        assertThat(exception.getMessage()).contains("자동이체 정보를 찾을 수 없습니다");

        verify(autoPaymentRepository).findById(1L);
    }

    @Test
    @DisplayName("상태 변환 실패 - 잘못된 상태값 (INVALID_REQUEST)")
    void resolveStatus_Invalid() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.getAutoPaymentList(1L, "INVALID_STATUS","testuser"),
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(exception.getMessage()).contains("유효하지 않은 상태 값입니다");
    }

    @Test
    @DisplayName("자동이체 해지 성공")
    void cancelAutoPayment_Success() {
        // given
        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAutoPayment));
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));

        // when
        autoPaymentService.cancelAutoPayment(1L, 1L,"testuser");

        // then
        assertThat(mockAutoPayment.getProcessingStatus())
                .isEqualTo(AutoPaymentStatus.CANCELLED);

        verify(autoPaymentRepository).findById(1L);
        verify(edubankapiAccountRepository).findById(1L);
    }

    @Test
    @DisplayName("자동이체 해지 실패 - 존재하지 않음 (ENTITY_NOT_FOUND)")
    void cancelAutoPayment_Fail_NotFound() {
        // given
        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.cancelAutoPayment(1L, 1L,"testuser"),
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
        assertThat(exception.getMessage()).contains("자동이체 정보를 찾을 수 없습니다");

        verify(autoPaymentRepository).findById(1L);
    }

    @Test
    @DisplayName("자동이체 해지 실패 - 소유자 불일치 시 존재 여부 숨김 (FORBIDDEN)")
    void cancelAutoPayment_Fail_NotOwner() {
        // given
        Users otherUser = Users.builder()
                .id(2L)
                .userId("otheruser")
                .authUser(AuthUsers.builder().userId("otheruser").build())
                .build();

        EducationalAccount otherAccount = EducationalAccount.builder()
                .id(2L)
                .accountNumber("1002-999-888777")
                .user(otherUser)
                .build();

        AutoPayment otherAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(otherAccount)  // 다른 계좌!
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(otherAutoPayment));
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(otherAccount));

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.cancelAutoPayment(1L, 2L,"testuser"),  // 다른 사람 계좌(2L)로 시도
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(exception.getMessage()).contains("접근 권한이 없습니다");

        verify(edubankapiAccountRepository).findById(2L);
    }

    @Test
    @DisplayName("자동이체 해지 실패 - 이미 해지됨 (INVALID_REQUEST)")
    void cancelAutoPayment_Fail_AlreadyCancelled() {
        // given
        AutoPayment cancelledAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .processingStatus(AutoPaymentStatus.CANCELLED)  // 이미 해지됨!
                .build();

        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(cancelledAutoPayment));
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));

        // when
        CommonException exception = catchThrowableOfType(
                () -> autoPaymentService.cancelAutoPayment(1L, 1L,"testuser"),
                CommonException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(exception.getMessage()).contains("이미 해지된 자동이체입니다");

        verify(autoPaymentRepository).findById(1L);
        verify(edubankapiAccountRepository).findById(1L);
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 지정일 31일 (모든 월에서 동작)")
    void createAutoPayment_DesignatedDate31_Success() {
        // given
        AutoPaymentCreateRequest requestWith31 = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1,
                31,  // 31일 지정 (2월에는 28/29일에 실행될 예정)
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(mockAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(requestWith31,"testuser");

        // then
        assertThat(response).isNotNull();
        verify(edubankapiAccountRepository, times(2)).findById(1L);
        verify(autoPaymentRepository).save(any(AutoPayment.class));
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 지정일 1일 (경계값)")
    void createAutoPayment_DesignatedDate1_Success() {
        // given
        AutoPaymentCreateRequest requestWith1 = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1,
                1,  // 1일 지정
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(mockAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(requestWith1,"testuser");

        // then
        assertThat(response).isNotNull();
        verify(edubankapiAccountRepository, times(2)).findById(1L);
        verify(autoPaymentRepository).save(any(AutoPayment.class));
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 윤년 2월 29일 만료")
    void createAutoPayment_LeapYearFebruary29_Success() {
        // given
        AutoPaymentCreateRequest leapYearRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 28,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 29),  // 윤년 2월 29일
                "1234"
        );

        AutoPayment leapYearAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(50000)
                .counterpartyName("홍길동")
                .displayName("월세")
                .transferCycle(1)
                .designatedDate(28)
                .startDate(LocalDate.of(2024, 1, 1))
                .expirationDate(LocalDate.of(2024, 2, 29))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(leapYearAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(leapYearRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.expirationDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        verify(edubankapiAccountRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 시작일과 만료일 1년 차이")
    void createAutoPayment_OneYearPeriod_Success() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate expirationDate = LocalDate.of(2026, 1, 1);  // 정확히 1년 후

        AutoPaymentCreateRequest oneYearRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동", "월세", 1, 5,
                startDate,
                expirationDate,
                "1234"
        );

        AutoPayment oneYearAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(50000)
                .counterpartyName("홍길동")
                .displayName("월세")
                .transferCycle(1)
                .designatedDate(5)
                .startDate(startDate)
                .expirationDate(expirationDate)
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(oneYearAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(oneYearRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.expirationDate()).isEqualTo(expirationDate);
        verify(edubankapiAccountRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 금액 1원 (최소값)")
    void createAutoPayment_Amount1Won_Success() {
        // given
        AutoPaymentCreateRequest minAmountRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789",
                1,  // 1원
                "홍길동", "테스트", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        AutoPayment minAmountAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(1)
                .counterpartyName("홍길동")
                .displayName("테스트")
                .transferCycle(1)
                .designatedDate(5)
                .startDate(LocalDate.of(2025, 1, 1))
                .expirationDate(LocalDate.of(2025, 12, 31))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(minAmountAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(minAmountRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(1);
        verify(edubankapiAccountRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 금액 100만원")
    void createAutoPayment_Amount1Million_Success() {
        // given
        AutoPaymentCreateRequest largeAmountRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789",
                1_000_000,  // 100만원
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        AutoPayment largeAmountAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(1_000_000)
                .counterpartyName("홍길동")
                .displayName("월세")
                .transferCycle(1)
                .designatedDate(5)
                .startDate(LocalDate.of(2025, 1, 1))
                .expirationDate(LocalDate.of(2025, 12, 31))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(largeAmountAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(largeAmountRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(1_000_000);
        verify(edubankapiAccountRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 표시 이름 30자 (최대)")
    void createAutoPayment_DisplayName30Chars_Success() {
        // given
        String maxLengthName = "가".repeat(30);  // 30자
        AutoPaymentCreateRequest maxNameRequest = new AutoPaymentCreateRequest(
                1L, "020", "110-123-456789", 50000,
                "홍길동",
                maxLengthName,  // 30자
                1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        AutoPayment maxNameAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(50000)
                .counterpartyName("홍길동")
                .displayName(maxLengthName)
                .transferCycle(1)
                .designatedDate(5)
                .startDate(LocalDate.of(2025, 1, 1))
                .expirationDate(LocalDate.of(2025, 12, 31))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(maxNameAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(maxNameRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.displayName()).hasSize(30);
        verify(edubankapiAccountRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("자동이체 등록 성공 - 입금 계좌번호 20자 (최대)")
    void createAutoPayment_DepositNumber20Chars_Success() {
        // given
        String maxAccountNumber = "1".repeat(20);  // 20자
        AutoPaymentCreateRequest maxAccountRequest = new AutoPaymentCreateRequest(
                1L, "020",
                maxAccountNumber,  // 20자
                50000,
                "홍길동", "월세", 1, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "1234"
        );

        AutoPayment maxAccountAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber(maxAccountNumber)
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

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(autoPaymentRepository.save(any(AutoPayment.class)))
                .willReturn(maxAccountAutoPayment);

        // when
        AutoPaymentResponse response = autoPaymentService.createAutoPayment(maxAccountRequest,"testuser");

        // then
        assertThat(response).isNotNull();
        assertThat(response.depositNumber()).hasSize(20);
        verify(edubankapiAccountRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("자동이체 해지 - 등록 직후 즉시 해지 성공")
    void cancelAutoPayment_ImmediatelyAfterCreate_Success() {
        // given
        AutoPayment freshAutoPayment = AutoPayment.builder()
                .id(1L)
                .educationalAccount(mockAccount)
                .depositNumber("110-123-456789")
                .depositBankCode("020")
                .amount(50000)
                .counterpartyName("홍길동")
                .displayName("월세")
                .transferCycle(1)
                .designatedDate(5)
                .startDate(LocalDate.now())  // 오늘 등록
                .expirationDate(LocalDate.now().plusYears(1))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(freshAutoPayment));
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));

        // when
        autoPaymentService.cancelAutoPayment(1L, 1L,"testuser");

        // then
        assertThat(freshAutoPayment.getProcessingStatus())
                .isEqualTo(AutoPaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("자동이체 해지 - 만료일 하루 전 해지 성공")
    void cancelAutoPayment_OneDayBeforeExpiration_Success() {
        // given
        AutoPayment almostExpiredAutoPayment = AutoPayment.builder()
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
                .expirationDate(LocalDate.now().plusDays(1))  // 내일 만료
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(autoPaymentRepository.findById(anyLong()))
                .willReturn(Optional.of(almostExpiredAutoPayment));
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));

        // when
        autoPaymentService.cancelAutoPayment(1L, 1L,"testuser");

        // then
        assertThat(almostExpiredAutoPayment.getProcessingStatus())
                .isEqualTo(AutoPaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("자동이체 목록 조회 - 빈 목록 반환")
    void getAutoPaymentList_EmptyList_Success() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountId(anyLong()))
                .willReturn(List.of());  // 빈 리스트

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "ALL","testuser");

        // then
        assertThat(responses).isEmpty();
        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountId(1L);
    }

    @Test
    @DisplayName("자동이체 목록 조회 - CANCELLED 상태만 조회")
    void getAutoPaymentList_CancelledOnly_Success() {
        // given
        AutoPayment cancelledAutoPayment = AutoPayment.builder()
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
                .processingStatus(AutoPaymentStatus.CANCELLED)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                anyLong(), any(AutoPaymentStatus.class)))
                .willReturn(List.of(cancelledAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "CANCELLED","testuser");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).processingStatus()).isEqualTo("CANCELLED");
        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountIdAndProcessingStatus(
                1L, AutoPaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("자동이체 목록 조회 - 여러 건 조회")
    void getAutoPaymentList_MultipleItems_Success() {
        // given
        AutoPayment autoPayment1 = AutoPayment.builder()
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

        AutoPayment autoPayment2 = AutoPayment.builder()
                .id(2L)
                .educationalAccount(mockAccount)
                .depositNumber("110-999-888777")
                .depositBankCode("020")
                .amount(100000)
                .counterpartyName("김철수")
                .displayName("관리비")
                .transferCycle(1)
                .designatedDate(10)
                .startDate(LocalDate.of(2025, 1, 1))
                .expirationDate(LocalDate.of(2025, 12, 31))
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountId(anyLong()))
                .willReturn(List.of(autoPayment1, autoPayment2));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "ALL","testuser");

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AutoPaymentResponse::displayName)
                .containsExactly("월세", "관리비");
        verify(edubankapiAccountRepository).findById(1L);
    }

    @Test
    @DisplayName("상태 변환 - 소문자 'active' 입력 시 정상 처리")
    void resolveStatus_LowercaseActive_Success() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                anyLong(), any(AutoPaymentStatus.class)))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "active","testuser");

        // then
        assertThat(responses).hasSize(1);
        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountIdAndProcessingStatus(
                1L, AutoPaymentStatus.ACTIVE);
    }

    @Test
    @DisplayName("상태 변환 - 대소문자 섞인 'AcTiVe' 입력 시 정상 처리")
    void resolveStatus_MixedCaseActive_Success() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                anyLong(), any(AutoPaymentStatus.class)))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "AcTiVe","testuser");

        // then
        assertThat(responses).hasSize(1);
        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountIdAndProcessingStatus(
                1L, AutoPaymentStatus.ACTIVE);
    }

    @Test
    @DisplayName("상태 변환 - 빈 문자열 입력 시 ACTIVE로 처리")
    void resolveStatus_EmptyString_DefaultToActive() {
        // given
        given(edubankapiAccountRepository.findById(anyLong()))
                .willReturn(Optional.of(mockAccount));
        given(autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                anyLong(), any(AutoPaymentStatus.class)))
                .willReturn(List.of(mockAutoPayment));

        // when
        List<AutoPaymentResponse> responses = autoPaymentService.getAutoPaymentList(1L, "","testuser");

        // then
        assertThat(responses).hasSize(1);
        verify(edubankapiAccountRepository).findById(1L);
        verify(autoPaymentRepository).findByEducationalAccountIdAndProcessingStatus(
                1L, AutoPaymentStatus.ACTIVE);
    }
}