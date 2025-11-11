package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.common.HistoryStatus;
import dev.woori.wooriLearn.config.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<?>> getAll(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "ALL") HistoryStatus status,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sort,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId
    ) {
        PageResponse<PointsExchangeResponseDto> res = pointsExchangeService.getAllHistory(
                startDate, endDate, status, sort, page, size, userId
        );
        return ApiResponse.success(SuccessCode.OK, res);
    }
}
