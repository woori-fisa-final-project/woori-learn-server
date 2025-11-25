package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.dto.PointsUnifiedHistoryRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import dev.woori.wooriLearn.domain.account.service.PointsHistoryService;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointsExchangeService pointsExchangeService;
    private final PointsHistoryService pointsHistoryService;

    @PostMapping({"/points/exchange", "/api/points/exchange"})
    public ResponseEntity<BaseResponse<?>> requestExchange(
            @AuthenticationPrincipal String username,
            @RequestBody PointsExchangeRequestDto dto
    ) {
        return ApiResponse.success(SuccessCode.CREATED, pointsExchangeService.requestExchange(username, dto));
    }

    @GetMapping({"/api/points/history", "/points/history"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> getHistory(
            @AuthenticationPrincipal String principalUsername,
            @ModelAttribute PointsUnifiedHistoryRequestDto request
    ) {
        // 사용자 엔드포인트에서는 타 사용자 조회(userId 지정)를 허용하지 않음
        if (request.userId() != null) {
            throw new CommonException(ErrorCode.FORBIDDEN, "사용자 이력 조회에서는 userId를 지정할 수 없습니다.");
        }

        Page<PointsHistoryResponseDto> historyPage =
                pointsHistoryService.getUnifiedHistory(principalUsername, request, false)
                        .map(PointsHistoryResponseDto::new);

        return ApiResponse.success(SuccessCode.OK, historyPage);
    }
}
