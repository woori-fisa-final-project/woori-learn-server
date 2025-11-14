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

    public List<AutoPaymentResponse> getAutoPaymentList(Long educationalAccountId, String status, String currentUserId) {
        // 권한 체크: 요청한 계좌가 현재 사용자의 것인지 확인
        validateAccountOwnership(educationalAccountId, currentUserId);
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

    public AutoPaymentResponse getAutoPaymentDetail(Long autoPaymentId, String currentUserId) {
        AutoPayment autoPayment = autoPaymentRepository.findById(autoPaymentId)
                .orElseThrow(() -> {
                    log.error("자동이체 정보 조회 실패 - ID: {}", autoPaymentId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "자동이체 정보를 찾을 수 없습니다.");
                });

        // 권한 체크: 이 자동이체가 현재 사용자의 계좌에 속하는지 확인
        Long accountId = autoPayment.getEducationalAccount().getId();
        validateAccountOwnership(accountId, currentUserId);

        return AutoPaymentResponse.of(autoPayment, accountId);
    }

    @Transactional
    public AutoPaymentResponse createAutoPayment(AutoPaymentCreateRequest request, String currentUserId) {
        // 1. 권한 체크: 출금 계좌가 현재 사용자의 것인지 확인
        validateAccountOwnership(request.educationalAccountId(), currentUserId);

        // 2. 교육용 계좌 조회 및 검증
        EducationalAccount educationalAccount = findAndValidateAccount(
                request.educationalAccountId(),
                request.accountPassword()
        );
        // 2. 자동이체 생성
        AutoPayment autoPayment = AutoPayment.create(request, educationalAccount);

        // 3. 저장
        AutoPayment savedAutoPayment = autoPaymentRepository.save(autoPayment);

        log.info("자동이체 등록 완료 - ID: {}, 교육용계좌ID: {}",
                savedAutoPayment.getId(), request.educationalAccountId());

        return AutoPaymentResponse.of(savedAutoPayment, request.educationalAccountId());
    }

    @Transactional
    public AutoPayment cancelAutoPayment(Long autoPaymentId, Long educationalAccountId, String currentUserId) {
        log.info("자동이체 해지 시작 - 자동이체ID: {}, 교육용계좌ID: {}, 사용자ID: {}",
                autoPaymentId, educationalAccountId, currentUserId);

        // 1. 자동이체 조회
        AutoPayment autoPayment = autoPaymentRepository.findById(autoPaymentId)
                .orElseThrow(() -> {
                    log.error("자동이체 조회 실패 - ID: {}", autoPaymentId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "자동이체 정보를 찾을 수 없습니다.");
                });

        // 2. 권한 체크: 이 자동이체가 현재 사용자의 계좌에 속하는지 확인
        Long actualAccountId = autoPayment.getEducationalAccount().getId();
        validateAccountOwnership(actualAccountId, currentUserId);

        // 3. 소유자 확인 (educationalAccountId 파라미터 검증)
        if (!autoPayment.isOwnedBy(educationalAccountId)) {
            log.warn("자동이체 소유자 불일치 - 자동이체ID: {}, 요청계좌ID: {}, 실제계좌ID: {}",
                    autoPaymentId, educationalAccountId, actualAccountId);
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "자동이체 정보를 찾을 수 없습니다.");
        }

        // 3. 이미 해지된 경우
        if (autoPayment.isCancelled()) {
            log.warn("이미 해지된 자동이체 - ID: {}", autoPaymentId);
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "이미 해지된 자동이체입니다.");
        }

        // 4. 해지 처리
        autoPayment.cancel();

        log.info("자동이체 해지 완료 - ID: {}, 교육용계좌ID: {}", autoPaymentId, educationalAccountId);

        return autoPayment;

    }

    private EducationalAccount findAndValidateAccount(Long accountId, String password) {
        EducationalAccount account = edubankapiAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error("교육용 계좌 조회 실패 - ID: {}", accountId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "교육용 계좌를 찾을 수 없습니다.");
                });

        validateAccountPassword(account, password);
        return account;
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

    /**
     * 계좌 소유자 권한 검증
     * @param accountId 검증할 계좌 ID
     * @param currentUserId 현재 로그인한 사용자 ID (username)
     */
    private void validateAccountOwnership(Long accountId, String currentUserId) {
        EducationalAccount account = edubankapiAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error("교육용 계좌 조회 실패 - 계좌ID: {}", accountId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "교육용 계좌를 찾을 수 없습니다.");
                });

        // 계좌 소유자의 userId와 현재 사용자의 userId 비교
        String accountOwnerUserId = account.getUser().getUserId();
        if (!accountOwnerUserId.equals(currentUserId)) {
            log.warn("권한 없는 접근 시도 - 계좌ID: {}, 요청사용자: {}, 계좌소유자: {}",
                    accountId, currentUserId, accountOwnerUserId);
            throw new CommonException(ErrorCode.FORBIDDEN,
                    "접근 권한이 없습니다.");
        }

        log.debug("계좌 소유자 권한 검증 성공 - 계좌ID: {}, 사용자ID: {}", accountId, currentUserId);
    }
}