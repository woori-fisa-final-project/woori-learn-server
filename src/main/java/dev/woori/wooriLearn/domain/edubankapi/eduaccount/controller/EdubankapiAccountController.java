package dev.woori.wooriLearn.domain.edubankapi.eduaccount.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiAccountDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransactionHistoryDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.TransactionListReqDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiAccountService;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
     *   => [GET] /accounts/list/{userId}
     *   => 특정 사용자 {userId}의 모든 교육용 계좌를 조회
     *
     *   @param userId: 사용자 고유 ID
     *   @return : 계좌목록 (계좌명, 계좌번호, 잔액)
     **/
    @GetMapping("/list/{userId}")
    public ResponseEntity<BaseResponse<?>> getAccountsList(@PathVariable long userId){

        // 서비스 호출을 통해 userId 계좌 목록 조회
        List<EdubankapiAccountDto> accounts = edubankapiAccountService.getAccountByUserId(userId);

        // 조회 성공 응답 반환
        return ApiResponse.success(SuccessCode.OK, accounts);
    }

    /**
     *      거래내역 목록 조회
     *      조회 기간 : 1M, 3M, 6M, 1Y
     *      거래 구분 : ALL, DEPOSIT, WITHDRAW
     *      미선택 시 최근 1개월, 최신순 30건
     *
     *      @RequestParm : accountId, period, startDate, endDate, type가 쿼리로 들어오기 때문에 사용
     */
    @GetMapping("/transactions")
    public ResponseEntity<BaseResponse<?>> getTransactionList(TransactionListReqDto request) {
        List<EdubankapiTransactionHistoryDto> transactions =
                edubankapiAccountService.getTransactionList(
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
     */
    @PostMapping("/transfer")
    public ResponseEntity<BaseResponse<?>> transfer(@RequestBody EdubankapiTransferRequestDto request) {
        // 요청 본문(JSON)을 EdubankapiTransferRequestDto 객체로 매핑
        // @RequestBody: JSON → DTO 자동 변환

        //디버깅 및 추적용
        log.info("[계좌이체 요청]: {}", request);

        /*
             Service 계층의 transfer() 메서드 호출
             → 실제 계좌 잔액 검증, 출금 처리, 입금 처리, 거래내역 저장 등을 수행
             → 그 결과로 Response DTO 반환
         */
        return ApiResponse.success(SuccessCode.OK, transferService.transfer(request));
    }


}
