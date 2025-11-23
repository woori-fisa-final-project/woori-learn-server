package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/points/exchange", "/api/points/exchange"})
@RequiredArgsConstructor
public class PointsExchangeController {

    private final PointsExchangeService pointsExchangeService;


    @PostMapping
    public ResponseEntity<BaseResponse<?>> requestExchange(
            @AuthenticationPrincipal String username,
            @RequestBody PointsExchangeRequestDto dto
    ) {
        return ApiResponse.success(SuccessCode.CREATED, pointsExchangeService.requestExchange(username, dto));
    }
}
