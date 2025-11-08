package dev.woori.wooriLearn.domain.account.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.account.dto.AccountDto;
import dev.woori.wooriLearn.domain.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;



@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    // final을 통해 생성자 자동 주입
    private final AccountService accountService;

    /**
     *   postman
     *   => [GET] /accounts/list/{userId}
     *   => 특정 사용자 {userId}의 모든 교육용 계좌를 조회
     *
     *   @param userId: 사용자 고유 ID
     *   @return : 계좌목록 (계좌명, 계좌번호, 잔액)
     **/
    @GetMapping("/list/{userId}")
    public ResponseEntity<BaseResponse<?>> getAccountsList(@PathVariable long userId){

        // 서비스 호출을 통해 userId 계좌 목록 조회
        List<AccountDto> accounts = accountService.getAccountByUserId(userId);

        // 조회 성공 응답 반환
        return ApiResponse.success(SuccessCode.OK, accounts);
    }

}
