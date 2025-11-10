package dev.woori.wooriLearn.domain.edubankapi.eduaccount.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiAccountDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransactionHistoryDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class EdubankapiAccountController {

    // final을 통해 생성자 자동 주입
    private final EdubankapiAccountService edubankapiAccountService;


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
    public ResponseEntity<BaseResponse<?>> getTransactionList(
            @RequestParam Long accountId,                                           // 계좌번호
            @RequestParam(required = false) String period,                           // 조회기간 1M/3M/6M/1년, 없으면 1월
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // 직접 시작일 지정
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,   // 직접 종료일 지정
            @RequestParam(required = false, defaultValue = "ALL") String type        // 거래구분 필터 기본 전체 보기
            ) {
        List<EdubankapiTransactionHistoryDto> transactions =
                edubankapiAccountService.getTransactionList(accountId, period, startDate, endDate, type);

        return ApiResponse.success(SuccessCode.OK, transactions);
    }


}
