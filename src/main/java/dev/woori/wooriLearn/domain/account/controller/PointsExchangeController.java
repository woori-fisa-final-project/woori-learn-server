package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.domain.account.dto.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/points/exchange")
@RequiredArgsConstructor
public class PointsExchangeController {

    private final PointsExchangeService pointsExchangeService;

    @PostMapping("")
    public ResponseEntity<PointsExchangeResponseDto> requestExchange(@RequestBody PointsExchangeRequestDto dto) {
        PointsExchangeResponseDto response = pointsExchangeService.requestExchange(dto);
        return ResponseEntity.ok(response);
    }
}
