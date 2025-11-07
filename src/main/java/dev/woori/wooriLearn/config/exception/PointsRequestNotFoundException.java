package dev.woori.wooriLearn.config.exception;

public class PointsRequestNotFoundException extends CommonException {
    public PointsRequestNotFoundException(Long requestId) {
        super(ErrorCode.ENTITY_NOT_FOUND, "출금 요청을 찾을 수 없습니다. requestId=" + requestId);
    }
}
