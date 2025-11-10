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

    private final EdubankapiTransferService transferService;

    /**
     *      계좌이체 실행
     *      POST /accounts/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<BaseResponse<?>> transfer(@RequestBody EdubankapiTransferRequestDto request) {
        log.info("[계좌이체 요청]: {}", request);

        EdubankapiTransferResponseDto result = transferService.transfer(request);

        return ApiResponse.success(SuccessCode.OK, result);
    }
}
