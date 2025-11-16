package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.domain.account.dto.PointsUnifiedHistoryRequestDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.service.PointsHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points/history")
public class PointsHistoryController {

    private final PointsHistoryService service;

    @GetMapping
    public Page<PointsHistoryResponseDto> getHistory(
            @AuthenticationPrincipal String username,
            @ModelAttribute PointsUnifiedHistoryRequestDto request
    ) {
        return service.getUnifiedHistory(username, request)
                .map(PointsHistoryResponseDto::new);
    }
}
