package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.scenario.dto.*;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioProgressService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *  제공 기능:
 *  1) GET  /users/{userId}/scenarios/{id}/progress   : 저장된 위치가 있으면 그 스텝부터, 없으면 시작 스텝 반환
 *  2) POST /users/{userId}/scenarios/{id}/progress   : 현재 스텝 체크인 저장
 *  3) POST /users/{userId}/scenarios/{id}/advance    : 다음 스텝으로 진행(퀴즈 게이트 포함)
 *
 *  접근 제어:
 *   - 인증 필요 + 경로의 userId가 본인 또는 ADMIN만 접근 가능
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/users/{userId}/scenarios/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
)
public class ScenarioController {

    private final ScenarioProgressService progressService;
    private final UserRepository userRepository;

    /**
     * 사용자의 시나리오 진행 상태 조회(재개)
     * ex) GET /users/{userId}/scenarios/{id}/progress
     */
    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated() and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<BaseResponse<?>> resume(
            @AuthenticationPrincipal String username,
            @PathVariable String userId,
            @PathVariable("id") Long scenarioId
    ) {
        Users me = findUserByUserId(userId);
        return ApiResponse.success(SuccessCode.OK, progressService.resume(me, scenarioId));
    }

    /**
     * 현재 스텝 체크인 저장
     * ex) POST /users/{userId}/scenarios/{id}/progress
     * Body: { "nowStepId": 103 }
     */
    @PostMapping(value = "/progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<BaseResponse<?>> saveCheckpoint(
            @AuthenticationPrincipal String username,
            @PathVariable String userId,
            @PathVariable("id") Long scenarioId,
            @Valid @RequestBody ProgressSaveReqDto req
    ) {
        Users me = findUserByUserId(userId);
        progressService.saveCheckpoint(me, scenarioId, req.nowStepId());
        return ApiResponse.success(SuccessCode.OK);
    }

    /**
     * 다음 스텝으로 이동(퀴즈 게이트 포함)
     * ex) POST /users/{userId}/scenarios/{id}/advance
     * Body 예:
     *   - { "nowStepId": 102 }
     *   - { "nowStepId": 103 } (QUIZ_REQUIRED)
     *   - { "nowStepId": 103, "answer": 2 } (정답 시 ADVANCED)
     */
    @PostMapping(value = "/advance", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<BaseResponse<?>> advance(
            @AuthenticationPrincipal String username,
            @PathVariable String userId,
            @PathVariable("id") Long scenarioId,
            @Valid @RequestBody AdvanceReqDto req
    ) {
        Users me = findUserByUserId(userId);
        return ApiResponse.success(SuccessCode.OK,
                progressService.advance(me, scenarioId, req.nowStepId(), req.answer()));
    }

    private Users findUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다: " + userId));
    }
}
