package dev.woori.wooriLearn.config.exception;

public class InvalidParameterException extends CommonException {
    public InvalidParameterException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
