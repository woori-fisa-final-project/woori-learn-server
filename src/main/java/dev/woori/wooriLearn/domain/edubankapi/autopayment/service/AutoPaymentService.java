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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AutoPaymentService {

    private final AutoPaymentRepository autoPaymentRepository;
    private final EdubankapiAccountRepository edubankapiAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auto-payment.max-amount:5000000}")
    private int maxTransferAmount;

    @Value("${app.auto-payment.amount-limit-enabled:true}")
    private boolean amountLimitEnabled;

    private static final String ALL_STATUS = "ALL";

    private static final int END_OF_MONTH_CODE = 99;

    /**
     * 자동이체 목록 조회 (전체 조회 - 레거시)
     * @deprecated 페이징 처리된 getAutoPaymentListPaged() 사용 권장
     */
    @Deprecated
    @Cacheable(value = "autoPaymentList", key = "#currentUserId + ':' + #educationalAccountId + ':' + #status")
    public List<AutoPaymentResponse> getAutoPaymentList(Long educationalAccountId, String status, String currentUserId) {
        log.info("자동이체 목록 조회 (캐시 미스) - 계좌ID: {}, 상태: {}", educationalAccountId, status);

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

    /**
     * 자동이체 목록 조회 (페이징)
     *
     * Note: Page 타입은 Redis 직렬화 불가능하여 캐싱 제외
     * 필요 시 PageImpl을 직렬화 가능한 DTO로 변환하여 캐싱 가능
     */
    public Page<AutoPaymentResponse> getAutoPaymentListPaged(
            Long educationalAccountId,
            String status,
            String currentUserId,
            Pageable pageable) {

        log.info("자동이체 목록 조회 (페이징) - 계좌ID: {}, 상태: {}, 페이지: {}, 크기: {}",
                educationalAccountId, status, pageable.getPageNumber(), pageable.getPageSize());

        // 권한 체크: 요청한 계좌가 현재 사용자의 것인지 확인
        validateAccountOwnership(educationalAccountId, currentUserId);

        Page<AutoPayment> autoPayments;

        if (ALL_STATUS.equalsIgnoreCase(status)) {
            autoPayments = autoPaymentRepository.findByEducationalAccountId(educationalAccountId, pageable);
        } else {
            AutoPaymentStatus paymentStatus = resolveStatus(status);
            autoPayments = autoPaymentRepository.findByEducationalAccountIdAndProcessingStatus(
                    educationalAccountId, paymentStatus, pageable);
        }

        return autoPayments.map(autoPayment -> AutoPaymentResponse.of(autoPayment, educationalAccountId));
    }

    @Cacheable(value = "autoPaymentDetail", key = "#currentUserId + ':' + #autoPaymentId", unless = "#result == null")
    public AutoPaymentResponse getAutoPaymentDetail(Long autoPaymentId, String currentUserId) {
        // N+1 문제 방지: 교육용 계좌 및 사용자 정보를 한 번에 조회
        AutoPayment autoPayment = autoPaymentRepository.findByIdWithAccountAndUser(autoPaymentId)
                .orElseThrow(() -> {
                    log.error("자동이체 정보 조회 실패 - ID: {}", autoPaymentId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "자동이체 정보를 찾을 수 없습니다.");
                });

        // 권한 체크: 이 자동이체가 현재 사용자의 계좌에 속하는지 직접 검증 (추가 DB 조회 없음)
        EducationalAccount educationalAccount = autoPayment.getEducationalAccount();
        String accountOwnerUserId = educationalAccount.getUser().getUserId();

        log.info("자동이체 상세 조회 소유권 검증 - 자동이체ID: {}, 요청사용자: {}, 계좌소유자: {}",
                autoPaymentId, currentUserId, accountOwnerUserId);

        if (!accountOwnerUserId.equals(currentUserId)) {
            log.warn("권한 없는 접근 시도 - 자동이체ID: {}, 요청사용자: {}, 계좌소유자: {}",
                    autoPaymentId, currentUserId, accountOwnerUserId);
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "자동이체 정보를 찾을 수 없습니다.");
        }

        Long accountId = educationalAccount.getId();
        return AutoPaymentResponse.of(autoPayment, accountId);
    }

    /**
     * 자동이체 등록 (캐시 무효화)
     * 등록 시 해당 계좌의 모든 상태 캐시 삭제 (ACTIVE, CANCELLED, ALL)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "autoPaymentList", key = "#currentUserId + ':' + #request.educationalAccountId() + ':ACTIVE'"),
            @CacheEvict(value = "autoPaymentList", key = "#currentUserId + ':' + #request.educationalAccountId() + ':CANCELLED'"),
            @CacheEvict(value = "autoPaymentList", key = "#currentUserId + ':' + #request.educationalAccountId() + ':ALL'")
    })
    public AutoPaymentResponse createAutoPayment(AutoPaymentCreateRequest request, String currentUserId) {
        // 1. 금액 한도 검증
        validateAmountLimit(request.amount());

        // 2. 교육용 계좌 조회, 소유권 확인 및 비밀번호 검증 (DB 조회 1회로 최적화)
        EducationalAccount educationalAccount = findAndValidateAccountWithOwnership(
                request.educationalAccountId(),
                request.accountPassword(),
                currentUserId
        );

        // 3. 지정일 처리 로직 적용
        int finalDesignatedDate = resolveDesignatedDate(request);

        // 4. 자동이체 엔티티 생성
        AutoPayment autoPayment = AutoPayment.createWithResolvedDate(
                request,
                educationalAccount,
                finalDesignatedDate
        );

        // 5. 저장
        AutoPayment savedAutoPayment = autoPaymentRepository.save(autoPayment);

        log.info("자동이체 등록 완료 - ID: {}, 교육용계좌ID: {}, 최종지정일: {}",
                savedAutoPayment.getId(), request.educationalAccountId(), finalDesignatedDate);

        return AutoPaymentResponse.of(savedAutoPayment, request.educationalAccountId());
    }

    /**
     * 자동이체 해지 (캐시 무효화)
     * 해지 시 해당 계좌의 모든 상태 캐시 삭제 (ACTIVE, CANCELLED, ALL)
     * DB 조회 최적화: findByIdWithAccountAndUser() 사용하여 1회 조회
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "autoPaymentList", key = "#currentUserId + ':' + #educationalAccountId + ':ACTIVE'"),
            @CacheEvict(value = "autoPaymentList", key = "#currentUserId + ':' + #educationalAccountId + ':CANCELLED'"),
            @CacheEvict(value = "autoPaymentList", key = "#currentUserId + ':' + #educationalAccountId + ':ALL'"),
            @CacheEvict(value = "autoPaymentDetail", key = "#currentUserId + ':' + #autoPaymentId")
    })
    public AutoPayment cancelAutoPayment(Long autoPaymentId, Long educationalAccountId, String currentUserId) {
        log.info("자동이체 해지 시작 - 자동이체ID: {}, 교육용계좌ID: {}, 사용자ID: {}",
                autoPaymentId, educationalAccountId, currentUserId);

        // 1. 자동이체 조회 (계좌 및 사용자 정보 포함 - 1회 조회로 최적화)
        AutoPayment autoPayment = autoPaymentRepository.findByIdWithAccountAndUser(autoPaymentId)
                .orElseThrow(() -> {
                    log.error("자동이체 조회 실패 - ID: {}", autoPaymentId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "자동이체 정보를 찾을 수 없습니다.");
                });

        EducationalAccount account = autoPayment.getEducationalAccount();

        // 2. 소유자 일치 확인 (요청한 계좌 ID와 실제 계좌 ID 비교)
        if (!autoPayment.isOwnedBy(educationalAccountId)) {
            log.warn("자동이체 소유자 불일치 - 자동이체ID: {}, 요청계좌ID: {}, 실제계좌ID: {}",
                    autoPaymentId, educationalAccountId, account.getId());
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "자동이체 정보를 찾을 수 없습니다.");
        }

        // 3. 계좌 소유권 확인 (현재 사용자가 계좌 소유자인지 확인)
        String accountOwnerUserId = account.getUser().getUserId();
        if (!accountOwnerUserId.equals(currentUserId)) {
            log.warn("권한 없는 접근 시도 - 자동이체ID: {}, 요청사용자: {}, 계좌소유자: {}",
                    autoPaymentId, currentUserId, accountOwnerUserId);
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "자동이체 정보를 찾을 수 없습니다.");
        }

        // 4. 이미 해지된 경우
        if (autoPayment.isCancelled()) {
            log.warn("이미 해지된 자동이체 - ID: {}", autoPaymentId);
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "이미 해지된 자동이체입니다.");
        }

        // 5. 해지 처리
        autoPayment.cancel();

        log.info("자동이체 해지 완료 - ID: {}, 교육용계좌ID: {}", autoPaymentId, educationalAccountId);

        return autoPayment;
    }

    /**
     * 지정일(designatedDate)을 정책에 따라 실제 날짜로 변환합니다.
     * 99일 경우, 시작일(startDate)의 월을 기준으로 말일을 계산합니다.
     *
     * @param request 자동이체 등록 요청 DTO
     * @return 정책이 적용된 실제 지정일 (1 ~ 31)
     */
    private int resolveDesignatedDate(AutoPaymentCreateRequest request) {
        int designatedDate = request.designatedDate();
        LocalDate startDate = request.startDate();

        if (designatedDate == END_OF_MONTH_CODE) {
            // 99일 경우, 시작일(startDate)의 월의 마지막 날짜를 계산
            int lastDay = startDate.lengthOfMonth();
            log.debug("지정일 99 처리: 시작일 {}의 말일은 {}일입니다.", startDate, lastDay);
            return lastDay;
        }

        // 1~31일 경우, 그 값 그대로 사용
        return designatedDate;
    }

    /**
     * 계좌 조회, 소유권 확인, 비밀번호 검증을 한 번에 수행 (DB 조회 최적화)
     * @param accountId 계좌 ID
     * @param password 계좌 비밀번호
     * @param currentUserId 현재 사용자 ID
     * @return 검증된 EducationalAccount
     */
    private EducationalAccount findAndValidateAccountWithOwnership(Long accountId, String password, String currentUserId) {
        // N+1 문제 방지: User 정보를 JOIN FETCH로 한 번에 조회
        EducationalAccount account = edubankapiAccountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> {
                    log.error("교육용 계좌 조회 실패 - ID: {}", accountId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "교육용 계좌를 찾을 수 없습니다.");
                });

        // 소유권 확인
        String accountOwnerUserId = account.getUser().getUserId();
        log.info("계좌 소유권 검증 - 계좌ID: {}, 요청사용자: {}, 계좌소유자: {}",
                accountId, currentUserId, accountOwnerUserId);

        if (!accountOwnerUserId.equals(currentUserId)) {
            log.warn("권한 없는 접근 시도 - 계좌ID: {}, 요청사용자: {}, 계좌소유자: {}",
                    accountId, currentUserId, accountOwnerUserId);
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "교육용 계좌를 찾을 수 없습니다.");
        }

        // 비밀번호 검증
        validateAccountPassword(account, password);

        log.debug("계좌 검증 완료 - 계좌ID: {}, 사용자ID: {}", accountId, currentUserId);
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
        // N+1 문제 방지: User 정보를 JOIN FETCH로 한 번에 조회
        EducationalAccount account = edubankapiAccountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> {
                    log.error("교육용 계좌 조회 실패 - 계좌ID: {}", accountId);
                    return new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                            "교육용 계좌를 찾을 수 없습니다.");
                });

        // 계좌 소유자의 userId와 현재 사용자의 userId 비교
        String accountOwnerUserId = account.getUser().getUserId();
        log.info("계좌 소유권 검증 - 계좌ID: {}, 요청사용자: {}, 계좌소유자: {}",
                accountId, currentUserId, accountOwnerUserId);

        if (!accountOwnerUserId.equals(currentUserId)) {
            log.warn("권한 없는 접근 시도 - 계좌ID: {}, 요청사용자: {}, 계좌소유자: {}",
                    accountId, currentUserId, accountOwnerUserId);
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "교육용 계좌를 찾을 수 없습니다.");
        }

        log.debug("계좌 소유자 권한 검증 성공 - 계좌ID: {}, 사용자ID: {}", accountId, currentUserId);
    }

    /**
     * 자동이체 금액 한도 검증
     * @param amount 이체 금액
     */
    private void validateAmountLimit(Integer amount) {
        if (!amountLimitEnabled) {
            return;
        }

        if (amount > maxTransferAmount) {
            log.warn("자동이체 금액 한도 초과 - 요청금액: {}, 최대한도: {}", amount, maxTransferAmount);
            throw new CommonException(
                    ErrorCode.INVALID_REQUEST,
                    String.format("자동이체 금액은 1회 최대 %,d원까지 가능합니다. (요청: %,d원)",
                            maxTransferAmount, amount)
            );
        }

        log.debug("자동이체 금액 한도 검증 성공 - 금액: {}, 최대한도: {}", amount, maxTransferAmount);
    }
}