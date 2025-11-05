package dev.woori.wooriLearn.domain.account.controller;

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

    @PostMapping("")
    public ResponseEntity<PointsExchangeResponseDto> requestExchange( //환전 요청
            @RequestBody PointsExchangeRequestDto dto) {

        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(dto);
        return ResponseEntity.ok(response);
    }

    // ✅ 필터 적용된 환전 내역 조회
    @GetMapping("/{userId}")
    public ResponseEntity<List<PointsExchangeResponseDto>> getHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) String startDate,     // yyyy-MM-dd
            @RequestParam(required = false) String endDate,       // yyyy-MM-dd
            @RequestParam(required = false, defaultValue = "ALL") String status, // ALL/REQUESTED/DONE/FAILED
            @RequestParam(required = false, defaultValue = "DESC") String sort   // DESC=최신순, ASC=과거순
    ) {
        List<PointsExchangeResponseDto> history = pointsExchangeService
                .getHistory(userId, startDate, endDate, status, sort);

        return ResponseEntity.ok(history);
    }
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<PointsExchangeResponseDto> approveExchange(
            @PathVariable Long requestId
    ) {
        PointsExchangeResponseDto response = pointsExchangeService.approveExchange(requestId);
        return ResponseEntity.ok(response);
    }

}
