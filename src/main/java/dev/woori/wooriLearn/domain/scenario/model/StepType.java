package dev.woori.wooriLearn.domain.scenario.model;

/**
 * DIALOG - 파란 배경에 캐릭터 (각 엔딩도 여기에 포함)
 * CHOICE - 선택지
 * OVERLAY - 오버레이에 캐릭터
 * MODAL - 오버레이에 모달
 * ETC - 그 외 (핸드폰 문자 화면, '6개월 후'화면, 포인트 적립 화면)
 */
public enum StepType {
    DIALOG,
    CHOICE,
    OVERLAY,
    MODAL,
    ETC
}
