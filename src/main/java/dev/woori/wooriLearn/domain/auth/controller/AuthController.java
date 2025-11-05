package dev.woori.wooriLearn.domain.auth.controller;


import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.auth.dto.LogoutReqDto;
import dev.woori.wooriLearn.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriLearn.domain.auth.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.auth.service.AuthService;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<?>> signup(@RequestBody SignupReqDto signupReqDto) {
        return ApiResponse.success(SuccessCode.OK, authService.signup(signupReqDto));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<?>> login(@RequestBody LoginReqDto loginReqDto) {
        return ApiResponse.success(SuccessCode.OK, authService.login(loginReqDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(@RequestBody RefreshReqDto refreshToken) {
        return ApiResponse.success(SuccessCode.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(@RequestBody LogoutReqDto logoutReqDto) {
        return ApiResponse.success(SuccessCode.OK, authService.logout(logoutReqDto));
    }
}
