package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.dto.PointsUnifiedHistoryRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsDepositService;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import dev.woori.wooriLearn.domain.account.service.PointsHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminPointController {

    private final PointsDepositService pointsDepositService;
    private final PointsExchangeService pointsExchangeService;
    private final PointsHistoryService pointsHistoryService;

    // 관리자 포인트 적립 (기존 경로 유지)
    @PostMapping({"/points/deposit", "/api/points/deposit"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<?>> deposit(
            @AuthenticationPrincipal String username,
            @RequestBody PointsDepositRequestDto dto
    ) {
        return ApiResponse.success(SuccessCode.OK, pointsDepositService.depositPoints(username, dto));
    }

    // 관리자 환전 승인 (기존 경로 유지)
    @PutMapping("/admin/points/exchange/approve/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<?>> approve(@PathVariable Long requestId) {
        return ApiResponse.success(SuccessCode.OK, pointsExchangeService.approveExchange(requestId));
    }

    // 관리자 환전 대기 목록 (기존 경로 유지)
    @GetMapping("/admin/points/exchange/apply")
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

    // 관리자 포인트 이력 조회 (userId 지정 가능)
    @GetMapping("/admin/points/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<?>> getHistoryForAdmin(
            @AuthenticationPrincipal String principalUsername,
            @ModelAttribute PointsUnifiedHistoryRequestDto request
    ) {
        Page<PointsHistoryResponseDto> historyPage =
                pointsHistoryService.getUnifiedHistory(principalUsername, request, true)
                        .map(PointsHistoryResponseDto::new);
        return ApiResponse.success(SuccessCode.OK, historyPage);
    }
}
