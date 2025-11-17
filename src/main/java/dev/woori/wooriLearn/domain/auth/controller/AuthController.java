package dev.woori.wooriLearn.domain.auth.controller;


import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import dev.woori.wooriLearn.domain.auth.dto.ChangePasswdReqDto;
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
    public ResponseEntity<BaseResponse<?>> login(@RequestBody LoginReqDto loginReqDto) {
        return ApiResponse.success(SuccessCode.OK, authService.login(loginReqDto));
    }

    @GetMapping("/verify")
    public ResponseEntity<BaseResponse<?>> verify(@RequestParam String userId) {
        authService.verify(userId);
        return ApiResponse.success(SuccessCode.OK, "사용 가능한 id입니다.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(@RequestBody RefreshReqDto refreshToken) {
        return ApiResponse.success(SuccessCode.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(Principal principal) {
        return ApiResponse.success(SuccessCode.OK, authService.logout(principal.getName()));
    }

    @PutMapping("/password")
    public ResponseEntity<BaseResponse<?>> changePassword(Principal principal,
                                                          @RequestBody ChangePasswdReqDto request) {
        authService.changePassword(principal.getName(), request);
        return ApiResponse.success(SuccessCode.OK);
    }
}
