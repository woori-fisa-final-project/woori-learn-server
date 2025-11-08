package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;

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
    private final UserRepository userRepository;

    @PostMapping("")
    public ResponseEntity<BaseResponse<?>> requestExchange(
            @AuthenticationPrincipal Object principal,
            @RequestBody PointsExchangeRequestDto dto
    ) {
        Long userId = getUserIdFromPrincipal(principal);
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
        Long userId = getUserIdFromPrincipal(principal);
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

    private Long getUserIdFromPrincipal(Object principal) {
        String username = extractUsername(principal);
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
        return user.getId();
    }

    private String extractUsername(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) return ud.getUsername();
        if (principal instanceof java.security.Principal p) return p.getName();
        if (principal instanceof String s && !"anonymousUser".equals(s)) return s;
        throw new CommonException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
