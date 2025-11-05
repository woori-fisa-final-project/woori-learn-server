package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.domain.account.dto.WithdrawRequestDto;
import dev.woori.wooriLearn.domain.account.dto.WithdrawResponseDto;
import dev.woori.wooriLearn.domain.account.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/points/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping("")
    public ResponseEntity<WithdrawResponseDto> requestWithdraw(@RequestBody WithdrawRequestDto dto) {
        WithdrawResponseDto response = withdrawService.requestWithdraw(dto);
        return ResponseEntity.ok(response);
    }
}
