package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.domain.common.auth.PrincipalUtils;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.common.HistoryStatus;
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

    @PostMapping
    public ResponseEntity<BaseResponse<?>> requestExchange(
            @AuthenticationPrincipal Object principal,
            @RequestBody PointsExchangeRequestDto dto
    ) {
        String username = PrincipalUtils.requireUsername(principal);
        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(username, dto);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<?>> getHistory(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "ALL") HistoryStatus status,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sort
    ) {
        String username = PrincipalUtils.requireUsername(principal);
        List<PointsExchangeResponseDto> history = pointsExchangeService
                .getHistory(username, startDate, endDate, status, sort);
        return ApiResponse.success(SuccessCode.OK, history);
    }


}
