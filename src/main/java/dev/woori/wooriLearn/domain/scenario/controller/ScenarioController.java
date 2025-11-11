package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.scenario.dto.*;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioProgressService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *  제공 기능:
 *  1) /progress    : 사용자의 재개 지점 조회(진행 있다면 해당 스텝 반환)
 *  2) /progress    : (POST) 현재 스텝으로 체크인
 *  3) /advance     : 다음 스텝으로 진행(퀴즈가 있다면 답 검증 게이트 처리 포함)
 */

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/me/scenarios/{scenarioId}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
)
public class ScenarioController {

    private final ScenarioProgressService progressService;
    private final UserRepository userRepository;

    /**
     * 나의 시나리오 진행 상태 조회(재개)
     * ex) GET /me/scenarios/1/progress
     * - 저장된 진행 기록이 있으면 해당 스텝부터
     * - 없으면 시작 스텝 반환
     */
    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> resume(
            @AuthenticationPrincipal String username,
            @PathVariable Long scenarioId
    ) {
        Users me = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다: " + username));
        return ApiResponse.success(SuccessCode.OK, progressService.resume(me, scenarioId));
    }

    /**
     * 현재 스텝에 체크인으로 저장
     * ex) POST /users/user/scenarios/1/progress
     * Body: { "nowStepId": 103 }
     *
     * - 필수: nowStepId
     * - 동작: (user, scenario) 기준 진행 레코드를 upsert하여 nowStepId 스텝으로 위치 저장
     */
    @PostMapping(value = "/progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> saveCheckpoint(
            @AuthenticationPrincipal String username,
            @PathVariable Long scenarioId,
            @Valid @RequestBody ProgressSaveReqDto req
    ) {
        Users me = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다: " + username));
        progressService.saveCheckpoint(me, scenarioId, req.nowStepId());
        return ApiResponse.success(SuccessCode.OK);
    }

    /**
     * 다음 스텝으로 이동
     * ex) POST /users/user/scenarios/1/advance
     * Body:
     *      - 퀴즈 없는 스텝: { "nowStepId": 102 }
     *      - 퀴즈 있는 스텝 1차: { "nowStepId": 103 } -> QUIZ_REQUIRED 응답 (퀴즈 정보 포함)
     *      - 퀴즈 있는 스텝 2차(정답 제출): { "nowStepId": 103, "answer": 2 } -> 정답이면 다음 스텝으로 진행
     *
     * 응답(AdvanceResDto):
     *      - status = ADVANCED      : 다음 스텝으로 이동 완료 (step = 다음 스텝)
     *      - status = QUIZ_REQUIRED : 퀴즈 있는 스텝에서 정답 미제출(quiz 포함)
     *      - status = QUIZ_WRONG    : 오답 (quiz 포함)
     *      - status = COMPLETED     : 마지막 스텝에서 완료 처리 (step, quiz = null)
     */
    @PostMapping(value = "/advance", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> advance(
            @AuthenticationPrincipal String username,
            @PathVariable Long scenarioId,
            @Valid @RequestBody AdvanceReqDto req
    ) {
        Users me = userRepository.findByUserId(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다: " + username));
        return ApiResponse.success(SuccessCode.OK, progressService.advance(me, scenarioId, req.nowStepId(), req.answer()));
    }
}
