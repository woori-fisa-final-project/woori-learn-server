package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.security.CurrentUserResolver;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/points/exchange")
@RequiredArgsConstructor
public class PointsExchangeController {

    private final PointsExchangeService pointsExchangeService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("")
    public ResponseEntity<BaseResponse<?>> requestExchange(
            @AuthenticationPrincipal Object principal,
            @RequestBody PointsExchangeRequestDto dto
    ) {
        Long userId = currentUserResolver.requireUserId(principal);
        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(userId, dto);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<?>> getHistory(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "DESC") String sort
    ) {
        Long userId = currentUserResolver.requireUserId(principal);
        List<PointsExchangeResponseDto> history = pointsExchangeService
                .getHistory(userId, startDate, endDate, status, sort);
        return ApiResponse.success(SuccessCode.OK, history);
    }

    // 관리자 승인 엔드포인트는 /admin 경로로 이동되었습니다.
}
