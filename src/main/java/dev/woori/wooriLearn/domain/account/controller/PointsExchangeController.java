package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.domain.account.dto.PointsHistorySearchRequestDto;
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
        return ApiResponse.success(SuccessCode.CREATED, pointsExchangeService.requestExchange(username, dto));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<?>> getHistory(
            @AuthenticationPrincipal Object principal,
            PointsHistorySearchRequestDto request
    ) {
        String username = PrincipalUtils.requireUsername(principal);

        List<PointsExchangeResponseDto> history = pointsExchangeService.getHistory(
                username,
                request.getStartDate(),
                request.getEndDate(),
                request.getPeriod(),
                request.getStatus(),
                request.getSort()
        );

        return ApiResponse.success(SuccessCode.OK, history);
    }



}
