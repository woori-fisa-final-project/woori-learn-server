package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.domain.scenario.dto.ScenarioDocDto;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 *  제공 기능:
 *  1) /doc: 시나리오 전체 문서 조회
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
    @PreAuthorize("permitAll()")
    public ResponseEntity<ScenarioDocDto> getDoc(@PathVariable Long scenarioId) {
        return ResponseEntity.ok()
                // 정적 리소스에 가까우므로 캐시 헤더를 통해 응답 재사용성 증대
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)))
                .body(docService.getScenarioDoc(scenarioId));
    }
}

