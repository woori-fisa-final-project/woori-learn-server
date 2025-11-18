package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자의 시나리오 진행 현황(완료/진행률)을 조회하는 컨트롤러
 *
 * BaseURL : /users/me/scenarios
 *  - /completed : 완료한 시나리오 목록 조회
 *  - /progress : 시나리오의 진행률 조회
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users/me/scenarios")
public class ScenarioStatusController {

    private final ScenarioStatusService scenarioStatusService;

    @GetMapping("/completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> getMyCompletedScenarios(
            @AuthenticationPrincipal String username
    ) {
        return ApiResponse.success(SuccessCode.OK, scenarioStatusService.getCompletedScenarios(username));
    }

    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> getMyScenarioProgress(
            @AuthenticationPrincipal String username
    ) {
        return ApiResponse.success(SuccessCode.OK, scenarioStatusService.getScenarioProgress(username));
    }
}