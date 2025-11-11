package dev.woori.wooriLearn.domain.edubankapi.eduaccount.controller;

import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferResponseDto;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.service.EdubankapiTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class EdubankapiTransferController {

    // 실제 계좌이체 로직(출금, 입금, 예외처리 등)은 Service에서 수행
    private final EdubankapiTransferService transferService;

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
        EdubankapiTransferResponseDto result = transferService.transfer(request);

        // 성공 결과 반환
        return ApiResponse.success(SuccessCode.OK, result);
    }
}
