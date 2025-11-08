package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsDepositService;
import dev.woori.wooriLearn.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/points/deposit")
@RequiredArgsConstructor
public class PointsDepositController {

    private final PointsDepositService pointsDepositService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<?>> deposit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PointsDepositRequestDto dto
    ) {
        Long userId = userDetails.getId();
        return ApiResponse.success(SuccessCode.CREATED, pointsDepositService.depositPoints(userId, dto));
    }
}
