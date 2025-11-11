package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.domain.scenario.dto.*;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioDocService;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioProgressService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 *  제공 기능:
 *  1) /doc        : 시나리오 전체 문서 조회
 *  2) /progress   : 사용자의 재개 지점 조회(진행 저장 없으면 시작 스텝 반환)
 *  3) /progress   : (POST) 현재 스텝을 저장(체크포인트)
 *  4) /advance    : 다음 스텝으로 진행(퀴즈가 있으면 정답 검사/게이트 처리 포함)
 */

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/users/{userKey}/scenarios/{scenarioId}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
)
public class ScenarioController {

    private final ScenarioDocService docService;
    private final ScenarioProgressService progressService;
    private final UserRepository userRepository;

    /**
     * 시나리오 전체 문서 조회
     * ex) GET /users/user/scenarios/1/doc
     */
    @GetMapping("/doc")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ScenarioDocDto> getDoc(
            @PathVariable String userKey,
            @PathVariable Long scenarioId
    ) {
        return ResponseEntity.ok(docService.getScenarioDoc(scenarioId));
    }

    /**
     * 사용자의 시나리오 진행 상태를 조회(재개)
     * ex) GET /users/user/scenarios/1/progress
     * - 저장된 진행 이력이 있으면 해당 스텝을,
     * - 없으면 시작 스텝을 반환
     */
    @GetMapping("/progress")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ProgressResumeResDto> resume(
            @PathVariable String userKey,
            @PathVariable Long scenarioId
    ) {
        Users user = resolveUserFlexible(userKey);
        return ResponseEntity.ok(progressService.resume(user, scenarioId));
    }

    /**
     * 현재 스텝을 체크포인트로 저장
     * ex) POST /users/user/scenarios/1/progress
     * Body: { "nowStepId": 103 }
     *
     * - 필수: nowStepId
     * - 동작: (user, scenario) 기준 진행 레코드를 upsert하여 nowStepId 스텝으로 덮어쓴다.
     */
    @PostMapping(value = "/progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> saveCheckpoint(
            @PathVariable String userKey,
            @PathVariable Long scenarioId,
            @RequestBody ProgressSaveReqDto req
    ) {
        if (req.nowStepId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nowStepId는 필수입니다.");
        Users user = resolveUserFlexible(userKey);
        progressService.saveCheckpoint(user, scenarioId, req.nowStepId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 다음 스텝으로 이동
     * ex) POST /users/user/scenarios/1/advance
     * Body:
     *      - 퀴즈 없는 스텝: { "nowStepId": 102 }
     *      - 퀴즈 있는 스텝 1차: { "nowStepId": 103 } → QUIZ_REQUIRED 응답 (퀴즈 정보 포함)
     *      - 퀴즈 있는 스텝 2차(정답 제출): { "nowStepId": 103, "answer": 2 } → 정답이면 다음 스텝으로 진행
     *
     * 응답(AdvanceResDto):
     *      - status = ADVANCED      : 다음 스텝으로 이동 완료 (step 필드에 다음 스텝)
     *      - status = QUIZ_REQUIRED : 퀴즈 있는 스텝에서 정답 미제출 (quiz 필드 포함)
     *      - status = QUIZ_WRONG    : 오답 (quiz 필드 포함)
     *      - status = COMPLETED     : 마지막 스텝에서 완료 처리 (step, quiz = null)
     */
    @PostMapping(value = "/advance", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("permitAll()")
    public ResponseEntity<AdvanceResDto> advance(
            @PathVariable String userKey,
            @PathVariable Long scenarioId,
            @RequestBody AdvanceReqDto req
    ) {
        if (req.nowStepId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nowStepId는 필수입니다.");
        Users user = resolveUserFlexible(userKey);
        return ResponseEntity.ok(progressService.advance(user, scenarioId, req.nowStepId(), req.answer()));
    }

    private Users resolveUserFlexible(String userKey) {
        if (userKey == null || userKey.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userKey가 필요합니다.");
        if (userKey.chars().allMatch(Character::isDigit)) {
            Long pk = Long.parseLong(userKey);
            return userRepository.findById(pk)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음: id=" + pk));
        }
        return userRepository.findByUserId(userKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음: user_id=" + userKey));
    }
}
