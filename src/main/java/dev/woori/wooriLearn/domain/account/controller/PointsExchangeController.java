package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.CustomUserDetails;
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

//    /** 포인트 환전 신청(로그인 구현 전 개발용) */
//    @PostMapping("")
//    public ResponseEntity<PointsExchangeResponseDto> requestExchange(
//            @AuthenticationPrincipal CustomUserDetails principal,
//            @RequestBody PointsExchangeRequestDto dto) {
//
//        Long userId = principal.getId();
//        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(userId, dto);
//        return ResponseEntity.ok(response);
//    }
    @PostMapping("{userId}")
    public ResponseEntity<PointsExchangeResponseDto> requestExchange(
            @RequestBody PointsExchangeRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = 1L;//테스트용
//        Long userId = userDetails.getId(); //  로그인된 사용자 ID
        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(userId, dto);
        return ResponseEntity.ok(response);
    }


    /** 특정 사용자 환전 내역 조회 (로그인 기능 없을 때 테스트용) */
    @GetMapping("/{userId}")//
    public ResponseEntity<List<PointsExchangeResponseDto>> getHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) String startDate,     // yyyy-MM-dd
            @RequestParam(required = false) String endDate,       // yyyy-MM-dd
            @RequestParam(required = false, defaultValue = "ALL") String status, //ALL/APPLY/SUCCESS/FAILED
            @RequestParam(required = false, defaultValue = "DESC") String sort
    ) {
        List<PointsExchangeResponseDto> history = pointsExchangeService
                .getHistory(userId, startDate, endDate, status, sort);

        return ResponseEntity.ok(history);
    }

    /** 환전 승인(관리자용) */
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<PointsExchangeResponseDto> approveExchange(
            @PathVariable Long requestId
    ) {
        PointsExchangeResponseDto response = pointsExchangeService.approveExchange(requestId);
        return ResponseEntity.ok(response);
    }

    // /** 로그인한 사용자 본인 환전 내역 조회 */ (로그인 구현 후 적용 예정)
    // @GetMapping("/me")
    // public ResponseEntity<List<PointsExchangeResponseDto>> getMyHistory(
    //         @AuthenticationPrincipal CustomUserDetails principal) {
    //
    //     Long userId = principal.getId();
    //     List<PointsExchangeResponseDto> history = pointsExchangeService.getHistory(userId);
    //     return ResponseEntity.ok(history);
    // }

}
