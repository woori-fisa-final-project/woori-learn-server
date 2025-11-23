package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/admin/points/exchange")
@RequiredArgsConstructor
public class AdminPointsExchangeController {

    private final PointsExchangeService pointsExchangeService;

    @PutMapping("/approve/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<?>> approve(@PathVariable Long requestId) {
        return ApiResponse.success(SuccessCode.OK, pointsExchangeService.approveExchange(requestId));
    }

    /**
     * 전체 회원의 환전 신청(APPLY 상태) 목록 조회 (관리자용)
     */
    @GetMapping("/apply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<?>> listPending(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                SuccessCode.OK,
                pointsExchangeService.getPendingWithdrawals(page, size)
        );
    }
}