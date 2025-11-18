package dev.woori.wooriLearn.domain.scenario.dto;

import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;

/**
 * 시나리오 진행 응답 DTO
 * 클라이언트의 진행 요청에 대한 서버 처리 결과를 전달
 *
 * @param status    진행 결과 상태값(ADVANCED/QUIZ_REQUIRED/QUIZ_WRONG/COMPLETED)
 * @param step      다음에 렌더링할 스텝 정보
 * @param quiz      퀴즈 표시/검증에 필요한 정보
 */
public record AdvanceResDto(
        AdvanceStatus status,
        ProgressResumeResDto step,
        QuizResDto quiz
) {}
