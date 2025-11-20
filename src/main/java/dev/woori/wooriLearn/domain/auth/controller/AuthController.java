package dev.woori.wooriLearn.domain.auth.controller;


import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.auth.dto.LoginResDto;
import dev.woori.wooriLearn.domain.auth.dto.RefreshResDto;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import dev.woori.wooriLearn.domain.auth.dto.ChangePasswdReqDto;
import dev.woori.wooriLearn.domain.auth.service.TokenWithCookie;
import dev.woori.wooriLearn.domain.auth.service.util.CookieUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<?>> login(@Valid @RequestBody LoginReqDto loginReqDto) {
        TokenWithCookie tokenWithCookie = authService.login(loginReqDto);
        LoginResDto loginResDto = new LoginResDto(tokenWithCookie.accessToken(), tokenWithCookie.role());
        return ApiResponse.successWithCookie(SuccessCode.OK, loginResDto, tokenWithCookie.cookie());
    }

    @GetMapping("/verify")
    public ResponseEntity<BaseResponse<?>> verify(@RequestParam String userId) {
        authService.verify(userId);
        return ApiResponse.success(SuccessCode.OK, "사용 가능한 id입니다.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(@CookieValue("refreshToken") String refreshToken) {
        TokenWithCookie tokenWithCookie = authService.refresh(refreshToken);
        RefreshResDto refreshResDto = new RefreshResDto(tokenWithCookie.accessToken());
        return ApiResponse.successWithCookie(SuccessCode.OK, refreshResDto, tokenWithCookie.cookie());
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(Principal principal) {
        authService.logout(principal.getName());
        return ApiResponse.successWithCookie(SuccessCode.OK, CookieUtil.deleteRefreshTokenCookie());
    }

    @PutMapping("/password")
    public ResponseEntity<BaseResponse<?>> changePassword(Principal principal,
                                                          @Valid @RequestBody ChangePasswdReqDto request) {
        authService.changePassword(principal.getName(), request);
        return ApiResponse.success(SuccessCode.OK);
    }
}
