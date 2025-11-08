package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/points/exchange")
@RequiredArgsConstructor
public class PointsExchangeController {

    private final PointsExchangeService pointsExchangeService;

    @PostMapping("/{userId}")
    public ResponseEntity<BaseResponse<?>> requestExchange(
            @PathVariable Long userId,
            @RequestBody PointsExchangeRequestDto dto
    ) {
        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(userId, dto);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<?>> getHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "DESC") String sort
    ) {
        List<PointsExchangeResponseDto> history = pointsExchangeService
                .getHistory(userId, startDate, endDate, status, sort);
        return ApiResponse.success(SuccessCode.OK, history);
    }

    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<BaseResponse<?>> approveExchange(
            @PathVariable Long requestId
    ) {
        PointsExchangeResponseDto response = pointsExchangeService.approveExchange(requestId);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}
