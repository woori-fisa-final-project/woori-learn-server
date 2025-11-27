package dev.woori.wooriLearn.domain.edubankapi.autopayment.controller;

import dev.woori.wooriLearn.util.AuthenticationUtil;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;

import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentCreateRequest;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.service.AutoPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/education/auto-payment")
@RequiredArgsConstructor
@Validated
public class AutoPaymentController {

    private final AutoPaymentService autoPaymentService;

    /**
     * 자동이체 목록 조회 (레거시 - 전체 조회)
     * @deprecated /list/paged 엔드포인트 사용 권장
     */
    @Deprecated
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<?>> getAutoPaymentList(
            @RequestParam @Positive(message = "교육용 계좌 ID는 양수여야 합니다.") Long educationalAccountId,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            Authentication authentication) {

        String currentUserId = AuthenticationUtil.extractUserId(authentication);
        log.info("자동이체 목록 조회 요청 - 교육용계좌ID: {}, 상태: {}, 사용자ID: {}",
                educationalAccountId, status, currentUserId);

        List<AutoPaymentResponse> response = autoPaymentService.getAutoPaymentList(
                educationalAccountId, status, currentUserId);

        return ApiResponse.success(SuccessCode.OK, response);
    }

    /**
     * 자동이체 목록 조회 (페이징 + 캐싱)
     * @param educationalAccountId 교육용 계좌 ID
     * @param status 처리 상태 (ACTIVE, CANCELLED, ALL) - 기본값: ACTIVE (활성 자동이체만 조회)
     */
    @GetMapping("/list/paged")
    public ResponseEntity<BaseResponse<?>> getAutoPaymentListPaged(
            @RequestParam @Positive(message = "교육용 계좌 ID는 양수여야 합니다.") Long educationalAccountId,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable){

        String currentUserId = AuthenticationUtil.extractUserId(authentication);
        Page<AutoPaymentResponse> response = autoPaymentService.getAutoPaymentListPaged(
                educationalAccountId, status, currentUserId, pageable);

        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/detail/{autoPaymentId}")
    public ResponseEntity<BaseResponse<?>> getAutoPaymentDetail(
            @PathVariable @Positive(message = "자동이체 ID는 양수여야 합니다.") Long autoPaymentId,
            Authentication authentication) {

        String currentUserId = AuthenticationUtil.extractUserId(authentication);
        log.info("자동이체 상세 조회 요청 - ID: {}, 사용자ID: {}", autoPaymentId, currentUserId);

        AutoPaymentResponse response = autoPaymentService.getAutoPaymentDetail(
                autoPaymentId, currentUserId);

        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping
    public ResponseEntity<BaseResponse<?>> createAutoPayment(
            @Valid @RequestBody AutoPaymentCreateRequest request,
            Authentication authentication) {

        String currentUserId = AuthenticationUtil.extractUserId(authentication);
        log.info("자동이체 등록 요청 - 교육용계좌ID: {}, 금액: {}, 사용자ID: {}",
                request.educationalAccountId(), request.amount(), currentUserId);

        AutoPaymentResponse response = autoPaymentService.createAutoPayment(
                request, currentUserId);

        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @PostMapping("/{autoPaymentId}/cancel")
    public ResponseEntity<BaseResponse<?>> cancelAutoPayment(
            @PathVariable @Positive(message = "자동이체 ID는 양수여야 합니다.") Long autoPaymentId,
            @RequestParam @Positive(message = "교육용 계좌 ID는 양수여야 합니다.") Long educationalAccountId,
            Authentication authentication) {

        String currentUserId = AuthenticationUtil.extractUserId(authentication);
        log.info("자동이체 해지 요청 - 자동이체ID: {}, 교육용계좌ID: {}, 사용자ID: {}",
                autoPaymentId, educationalAccountId, currentUserId);

        AutoPaymentResponse response = autoPaymentService.cancelAutoPayment(
                autoPaymentId, educationalAccountId, currentUserId);

        return ApiResponse.success(SuccessCode.OK, response);
    }

    /**
     * 특정 계좌의 모든 활성 자동이체 일괄 해지
     * 교육 초기화 또는 일괄 해지 기능에서 사용
     *
     * @param educationalAccountId 교육용 계좌 ID
     * @param authentication 인증 정보 (JWT 토큰)
     * @return 해지된 자동이체 건수
     */
    @PostMapping("/cancel-all")
    public ResponseEntity<BaseResponse<?>> cancelAllActiveAutoPayments(
            @RequestParam @Positive(message = "교육용 계좌 ID는 양수여야 합니다.") Long educationalAccountId,
            Authentication authentication) {

        String currentUserId = AuthenticationUtil.extractUserId(authentication);
        log.info("자동이체 일괄 해지 요청 - 교육용계좌ID: {}, 사용자ID: {}",
                educationalAccountId, currentUserId);

        int cancelledCount = autoPaymentService.cancelAllActiveAutoPayments(
                educationalAccountId, currentUserId);

        log.info("자동이체 일괄 해지 완료 - 교육용계좌ID: {}, 해지건수: {}",
                educationalAccountId, cancelledCount);

        return ApiResponse.success(SuccessCode.OK,
                java.util.Map.of(
                        "cancelledCount", cancelledCount,
                        "message", cancelledCount + "건의 자동이체가 해지되었습니다."
                ));
    }
}
