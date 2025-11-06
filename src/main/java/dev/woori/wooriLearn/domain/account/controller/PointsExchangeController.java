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

    /** 포인트 환전 신청 */
    @PostMapping("")
    public ResponseEntity<PointsExchangeResponseDto> requestExchange(@AuthenticationPrincipal CustomUserDetails principal,
                                                                     @RequestBody PointsExchangeRequestDto dto) {
        Long userId = principal.getId();               // 인증된 사용자 ID
        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(userId, dto);
        return ResponseEntity.ok(response);
    }





    /** 특정 사용자 환전 내역 조회 (로그인 기능 없을 때 테스트용) */
    @GetMapping("/{userId}")
    public ResponseEntity<List<PointsExchangeResponseDto>> getHistory(@PathVariable Long userId) {
        List<PointsExchangeResponseDto> history = pointsExchangeService.getHistory(userId);
        return ResponseEntity.ok(history);
    }
//    /** 현재 로그인한 사용자 본인의 환전 내역 조회 */ 로그인 구현 후 적용 예정
//    @GetMapping("/me")
//    public ResponseEntity<List<PointsExchangeResponseDto>> getMyHistory(
//            @AuthenticationPrincipal CustomUserDetails principal) {
//
//        Long userId = principal.getId();               // 인증된 사용자 ID
//        List<PointsExchangeResponseDto> history = pointsExchangeService.getHistory(userId);
//        return ResponseEntity.ok(history);
//    }
}
