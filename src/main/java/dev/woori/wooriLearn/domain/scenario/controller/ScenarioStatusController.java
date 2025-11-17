package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioStatusService;
import dev.woori.wooriLearn.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users/me/scenarios")
public class ScenarioStatusController {

    private final ScenarioStatusService scenarioStatusService;
    private final UserService userService;

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