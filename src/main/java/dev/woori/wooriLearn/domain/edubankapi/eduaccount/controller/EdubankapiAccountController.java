package dev.woori.wooriLearn.domain.edubankapi.eduaccount.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.*;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiAccountService;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/education/accounts")
@RequiredArgsConstructor
public class EdubankapiAccountController {

    // final을 통해 생성자 자동 주입
    private final EdubankapiAccountService edubankapiAccountService;
    // 실제 계좌이체 로직(출금, 입금, 예외처리 등)은 Service에서 수행
    private final EdubankapiTransferService transferService;

    /**
     *   계좌 목록 조회
     *   postman
     *   => [GET] /accounts/list
     *   => JWT 토큰에서 추출한 사용자의 모든 교육용 계좌를 조회
     *
     *   @param username: JWT 토큰에서 추출한 사용자 ID (username)
     *   @return : 계좌목록 (계좌명, 계좌번호, 잔액)
     **/
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<?>> getAccountsList(@AuthenticationPrincipal String username){

        // 서비스 호출을 통해 username으로 계좌 목록 조회
        List<EdubankapiAccountDto> accounts = edubankapiAccountService.getAccountByUsername(username);

        // 조회 성공 응답 반환
        return ApiResponse.success(SuccessCode.OK, accounts);
    }

    /**
     *      거래내역 목록 조회
     *      조회 기간 : 1M, 3M, 6M, 1Y
     *      거래 구분 : ALL, DEPOSIT, WITHDRAW
     *      미선택 시 최근 1개월, 최신순 30건
     *
     *      @param username JWT 토큰에서 추출한 사용자 ID
     *      @param request accountId, period, startDate, endDate, type 쿼리 파라미터
     */
    @GetMapping("/transactions")
    public ResponseEntity<BaseResponse<?>> getTransactionList(
            @AuthenticationPrincipal String username,
            @Valid TransactionListReqDto request) {

        List<EdubankapiTransactionHistoryDto> transactions =
                edubankapiAccountService.getTransactionList(
                        username,
                        request.accountId(),
                        request.periodOrDefault(),
                        request.startDate(),
                        request.endDate(),
                        request.typeOrDefault());

        return ApiResponse.success(SuccessCode.OK, transactions);
    }

    /**
     *      계좌이체 실행
     *      POST /accounts/transfer
     *      - 요청 Body: EdubankapiTransferRequestDto(JSON)
     *      - 응답 Body: EdubankapiTransferResponseDto(JSON)
     *
     *      @param username JWT 토큰에서 추출한 사용자 ID
     *      @param request 계좌이체 요청 정보
     */
    @PostMapping("/transfer")
    public ResponseEntity<BaseResponse<?>> transfer(
            @AuthenticationPrincipal String username,
            @RequestBody EdubankapiTransferRequestDto request) {

        /*
             Service 계층의 transfer() 메서드 호출
             → 출금 계좌 소유권 검증
             → 실제 계좌 잔액 검증, 출금 처리, 입금 처리, 거래내역 저장 등을 수행
             → 그 결과로 Response DTO 반환
         */
        return ApiResponse.success(SuccessCode.OK, transferService.transfer(username, request));
    }

    /**
     * 계좌 비밀번호 검증 (프론트엔드 Scenario5 요청 대응)
     * [POST] /education/accounts/transactions-password
     *
     * @param username : JWT 토큰 사용자 (본인 계좌인지 확인용)
     * @param request : { accountNumber, password }
     * @return : 비밀번호 일치 여부 (Boolean)
     */
    @PostMapping("/transactions-password")
    public ResponseEntity<BaseResponse<?>> checkAccountPassword(
            @AuthenticationPrincipal String username,
            @RequestBody PasswordCheckRequest request) {

        // 서비스 검증 로직 호출
        boolean isValid = edubankapiAccountService.checkPassword(username, request);

        // 결과 반환 (true 또는 false)
        return ApiResponse.success(SuccessCode.OK, isValid);
    }

}
