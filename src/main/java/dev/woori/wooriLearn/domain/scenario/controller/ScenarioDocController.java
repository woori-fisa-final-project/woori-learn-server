package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 *  제공 기능:
 *  1) GET  /scenarios/{scenarioId}/doc : 시나리오 전체 문서 조회
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/scenarios/{scenarioId}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
)
public class ScenarioDocController {

    private final ScenarioDocService docService;

    /**
     * 시나리오 전체 문서 조회
     * ex) GET /scenarios/1/doc
     */
    @GetMapping("/doc")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> getDoc(
            @PathVariable Long scenarioId) {
        return ApiResponse.successWithCache(
                SuccessCode.OK,
                docService.getScenarioDoc(scenarioId),
                Duration.ofMinutes(60)
        );
    }
}
