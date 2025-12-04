package dev.woori.wooriLearn.domain.account.entity;

public enum PointsStatus {
    APPLY, PROCESSING, SUCCESS, FAILED;

    public String message() {
        return switch (this) {
            case APPLY -> "출금 신청 처리 중입니다.";
            case PROCESSING -> "출금 요청을 처리 중입니다.";
            case SUCCESS -> "출금이 완료되었습니다.";
            case FAILED -> "출금 신청이 실패했습니다.";
        };
    }
}
