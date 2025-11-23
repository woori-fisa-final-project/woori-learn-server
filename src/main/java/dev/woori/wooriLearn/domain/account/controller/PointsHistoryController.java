package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsUnifiedHistoryRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points/history")
public class PointsHistoryController {

    private final PointsHistoryService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> getHistory(
            @AuthenticationPrincipal String principalUsername,
            @ModelAttribute PointsUnifiedHistoryRequestDto request
    ) {
        Page<PointsHistoryResponseDto> historyPage =
                service.getUnifiedHistory(principalUsername, request)
                        .map(PointsHistoryResponseDto::new);

        return ApiResponse.success(SuccessCode.OK, historyPage);
    }


}
