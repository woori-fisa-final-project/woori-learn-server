package dev.woori.wooriLearn.domain.user.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<?>> signup(@Valid @RequestBody SignupReqDto signupReqDto) {
        userService.signup(signupReqDto);
        return ApiResponse.success(SuccessCode.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<?>> getUserInfo(Principal principal) {
        return ApiResponse.success(SuccessCode.OK, userService.getUserInfo(principal.getName()));
    }

    @PutMapping("/nickname")
    public ResponseEntity<BaseResponse<?>> changeNickname(Principal principal,
                                                          @RequestBody ChangeNicknameReqDto request) {
        userService.changeNickname(principal.getName(), request);
        return ApiResponse.success(SuccessCode.OK);
    }
}
