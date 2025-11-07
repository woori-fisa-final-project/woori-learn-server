package dev.woori.wooriLearn.config.exception;

public class UserNotFoundException extends CommonException {
    public UserNotFoundException(Long userId) {
        super(ErrorCode.ENTITY_NOT_FOUND, "유저를 찾을 수 없습니다. userId=" + userId);
    }
}
