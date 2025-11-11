package dev.woori.wooriLearn.domain.scenario.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.scenario.service.ScenarioProgressService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/users/{userId}/scenarios/progresses",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
)
public class ScenarioProgressQueryController {

    private final ScenarioProgressService progressService;
    private final UserRepository userRepository;

    /**
     * 홈에 표시할 전체 진행 퍼센트 조회
     * ex) GET /users/{userId}/scenarios/progresses
     */
    @GetMapping
    @PreAuthorize("isAuthenticated() and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<BaseResponse<?>> overall(
            @AuthenticationPrincipal String username,
            @PathVariable String userId
    ) {
        Users me = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다: " + userId));

        return ApiResponse.success(SuccessCode.OK, progressService.computeOverallProgress(me));
    }
}

