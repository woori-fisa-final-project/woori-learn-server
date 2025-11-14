package dev.woori.wooriLearn.domain.edubankapi.autopayment.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;

import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentCreateRequest;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.service.AutoPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 인증 객체가 없으면 테스트 계정으로 처리
     */
    private String getCurrentUserId(Authentication authentication) {
        return (authentication != null) ? authentication.getName() : "1";  // 테스트용 사용자 ID
    }

    @GetMapping("/list")
    public ResponseEntity<BaseResponse<?>> getAutoPaymentList(
            @RequestParam @Positive(message = "교육용 계좌 ID는 양수여야 합니다.") Long educationalAccountId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        String currentUserId = getCurrentUserId(authentication);

        log.info("자동이체 목록 조회 요청 - 교육용계좌ID: {}, 상태: {}, 사용자ID: {}",
                educationalAccountId, status, currentUserId);

        List<AutoPaymentResponse> response = autoPaymentService.getAutoPaymentList(
                educationalAccountId, status, currentUserId);

        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/detail/{autoPaymentId}")
    public ResponseEntity<BaseResponse<?>> getAutoPaymentDetail(
            @PathVariable @Positive(message = "자동이체 ID는 양수여야 합니다.") Long autoPaymentId,
            Authentication authentication) {

        String currentUserId = getCurrentUserId(authentication);

        log.info("자동이체 상세 조회 요청 - ID: {}, 사용자ID: {}", autoPaymentId, currentUserId);

        AutoPaymentResponse response = autoPaymentService.getAutoPaymentDetail(
                autoPaymentId, currentUserId);

        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping
    public ResponseEntity<BaseResponse<?>> createAutoPayment(
            @Valid @RequestBody AutoPaymentCreateRequest request,
            Authentication authentication) {

        String currentUserId = getCurrentUserId(authentication);

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

        String currentUserId = getCurrentUserId(authentication);

        log.info("자동이체 해지 요청 - 자동이체ID: {}, 교육용계좌ID: {}, 사용자ID: {}",
                autoPaymentId, educationalAccountId, currentUserId);

        AutoPayment cancelledAutoPayment = autoPaymentService.cancelAutoPayment(
                autoPaymentId, educationalAccountId, currentUserId);

        AutoPaymentResponse response = AutoPaymentResponse.of(
                cancelledAutoPayment,
                cancelledAutoPayment.getEducationalAccount().getId()
        );

        return ApiResponse.success(SuccessCode.OK, response);
    }
}
