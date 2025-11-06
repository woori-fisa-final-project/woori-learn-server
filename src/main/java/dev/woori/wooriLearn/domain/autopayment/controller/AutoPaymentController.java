package dev.woori.wooriLearn.domain.autopayment.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;

import dev.woori.wooriLearn.domain.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.autopayment.service.AutoPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auto-payment")
@RequiredArgsConstructor
@Validated
public class AutoPaymentController {

    private final AutoPaymentService autoPaymentService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse<?>> getAutoPaymentList(
            @RequestParam @jakarta.validation.constraints.Positive(message = "교육용 계좌 ID는 양수여야 합니다.") Long educationalAccountId,
            @RequestParam(required = false) String status) {

        log.info("자동이체 목록 조회 요청 - 교육용계좌ID: {}, 상태: {}", educationalAccountId, status);

        List<AutoPaymentResponse> response = autoPaymentService.getAutoPaymentList(educationalAccountId, status);

        return ApiResponse.success(SuccessCode.OK, response);
    }
}