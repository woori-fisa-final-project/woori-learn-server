package dev.woori.wooriLearn.config.exception;

public class InvalidStateException extends CommonException {
    public InvalidStateException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
