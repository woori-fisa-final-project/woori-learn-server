package dev.woori.wooriLearn.domain.scenario.model;

/**
 * QUIZ_REQUIRED - 현재 스텝에 퀴즈가 있으며, 정답 제출 필요
 * QUIZ_WRONG - 제출한 정답이 오답
 * ADVANCED - 다음 스텝으로 정상 이동 완료
 * COMPLETED - 마지막 스텝을 통과하여 시나리오 완료
 */
public enum AdvanceStatus {
    QUIZ_REQUIRED,
    QUIZ_WRONG,
    CHOICE_REQUIRED,
    ADVANCED_FROZEN,
    BAD_ENDING,
    ADVANCED,
    COMPLETED
}
