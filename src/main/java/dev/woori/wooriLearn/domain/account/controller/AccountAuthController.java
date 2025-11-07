package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.AccountAuthDto;
import dev.woori.wooriLearn.domain.account.service.AccountAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 계좌 개설을 위한 본인인증(OTP) 관련 엔드포인트를 제공하는 컨트롤러
 *
 * Base URL: /account
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountAuthController {

    private final AccountAuthService service;

    /**
     * 인증번호(OTP) 발급/재발급 요청
     *
     * 흐름
     * 1) 사용자 식별자 추출
     * 2) 요청 DTO 유효성 검증
     * 3) 서비스로 위임 -> 외부 인증 서버 호출 -> OTP 수신/DB upsert -> 응답 반환
     */
    @PostMapping("/auth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountAuthDto.Response> request(
            // Security의 인증 주체에서 userId만 뽑아 사용 (예: username = userId)
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody AccountAuthDto.Request req
    ) {
        return ResponseEntity.ok(service.request(userId, req));
    }

    /**
     * 인증번호(OTP) 검증
     *
     * 흐름
     * 1) 사용자 식별자 호출
     * 2) 요청 DTO(code 6자리) 유효성 검증
     * 3) 서비스로 위임 -> DB에 저장된 OTP와 비교 -> 성공 시 레코드 삭제, 결과 반환
     */
    @PostMapping("/auth/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountAuthDto.VerifyResponse> verify(
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody AccountAuthDto.VerifyRequest req
    ) {
        return ResponseEntity.ok(service.verify(userId, req));
    }
}