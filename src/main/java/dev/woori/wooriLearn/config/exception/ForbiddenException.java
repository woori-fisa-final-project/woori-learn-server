package dev.woori.wooriLearn.config.exception;

public class ForbiddenException extends CommonException {
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
