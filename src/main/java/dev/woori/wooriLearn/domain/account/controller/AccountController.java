package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.AccountCreateReqDto;
import dev.woori.wooriLearn.domain.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/url")
    public ResponseEntity<BaseResponse<?>> getBankUrl(Principal principal) {
        String userId = principal.getName();
        return ApiResponse.success(SuccessCode.OK, accountService.getAccountUrl(userId));
    }

    @PostMapping("/created")
    public ResponseEntity<BaseResponse<?>> accountCreated(Principal principal, @RequestBody AccountCreateReqDto request) {
        String userId = principal.getName();
        accountService.registerAccount(userId, request);
        return ApiResponse.success(SuccessCode.CREATED);
    }
}
