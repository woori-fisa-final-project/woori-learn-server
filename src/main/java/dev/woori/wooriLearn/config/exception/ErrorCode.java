package dev.woori.wooriLearn.config.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 에러 통합 관리
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 400, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 40101, "토큰이 만료되었습니다."),
    TOKEN_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40102, "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 403, "접근 권한이 없습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "대상을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, 409, "중복된 자원입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, 429, "요청 한도를 초과했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 503, "처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),
    EXTERNAL_API_FAIL(HttpStatus.BAD_GATEWAY, 3001, "외부 서버 통신 실패");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
