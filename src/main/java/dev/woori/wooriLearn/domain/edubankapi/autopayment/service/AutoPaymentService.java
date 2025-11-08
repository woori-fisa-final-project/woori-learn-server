package dev.woori.wooriLearn.domain.edubankapi.autopayment.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentCreateRequest;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.repository.AutoPaymentRepository;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AutoPaymentService {

    private final AutoPaymentRepository autoPaymentRepository;
    private final EdubankapiAccountRepository edubankapiAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ALL_STATUS = "ALL";

    public List<AutoPaymentResponse> getAutoPaymentList(Long educationalAccountId, String status) {
        List<AutoPayment> autoPayments;

        if (ALL_STATUS.equalsIgnoreCase(status)) {
            autoPayments = autoPaymentRepository.findByEducationalAccountId(educationalAccountId);
        } else {
            AutoPaymentStatus paymentStatus = resolveStatus(status);
            autoPayments = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                    educationalAccountId, paymentStatus);
        }

        return autoPayments.stream()
                .map(autoPayment -> AutoPaymentResponse.of(autoPayment, educationalAccountId))
                .toList();
    }

    public AutoPaymentResponse getAutoPaymentDetail(Long autoPaymentId) {
        AutoPayment autoPayment = autoPaymentRepository.findById(autoPaymentId)
                .orElseThrow(() -> {
                    log.error("자동이체 정보 조회 실패 - ID: {}", autoPaymentId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "자동이체 정보를 찾을 수 없습니다.");
                });

        return AutoPaymentResponse.of(autoPayment, autoPayment.getEducationalAccount().getId());
    }

    @Transactional
    public AutoPaymentResponse createAutoPayment(AutoPaymentCreateRequest request) {
        // 1. 교육용 계좌 조회
        EducationalAccount educationalAccount = edubankapiAccountRepository
                .findById(request.educationalAccountId())
                .orElseThrow(() -> {
                    log.error("교육용 계좌 조회 실패 - ID: {}", request.educationalAccountId());
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "교육용 계좌를 찾을 수 없습니다.");
                });

        // 2. 계좌 비밀번호 검증
        validateAccountPassword(educationalAccount, request.accountPassword());

        // 3. 자동이체 엔티티 생성
        AutoPayment autoPayment = AutoPayment.builder()
                .educationalAccount(educationalAccount)
                .depositNumber(request.depositNumber())
                .depositBankCode(request.depositBankCode())
                .amount(request.amount())
                .counterpartyName(request.counterpartyName())
                .displayName(request.displayName())
                .transferCycle(request.transferCycle())
                .designatedDate(request.designatedDate())
                .startDate(request.startDate())
                .expirationDate(request.expirationDate())
                .processingStatus(AutoPaymentStatus.ACTIVE)
                .build();

        // 4. 저장
        AutoPayment savedAutoPayment = autoPaymentRepository.save(autoPayment);

        log.info("자동이체 등록 완료 - ID: {}, 교육용계좌ID: {}",
                savedAutoPayment.getId(), request.educationalAccountId());

        return AutoPaymentResponse.of(savedAutoPayment, request.educationalAccountId());
    }

    private void validateAccountPassword(EducationalAccount account, String inputPassword) {
        if (!passwordEncoder.matches(inputPassword, account.getAccountPassword())) {
            log.warn("계좌 비밀번호 불일치 - 계좌ID: {}", account.getId());
            throw new CommonException(ErrorCode.UNAUTHORIZED, "계좌 비밀번호가 일치하지 않습니다.");
        }
        log.debug("계좌 비밀번호 검증 성공 - 계좌ID: {}", account.getId());
    }

    private AutoPaymentStatus resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return AutoPaymentStatus.ACTIVE;
        }

        try {
            return AutoPaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 상태 값입니다. (사용 가능: " + AutoPaymentStatus.getAvailableValues() + ")");
        }
    }
}