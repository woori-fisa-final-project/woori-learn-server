package dev.woori.wooriLearn.example.controller;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.ApiResponse;
import dev.woori.wooriLearn.config.response.BaseResponse;
import dev.woori.wooriLearn.config.response.SuccessCode;
import dev.woori.wooriLearn.example.dto.TestDto;
import dev.woori.wooriLearn.example.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @GetMapping
    public ResponseEntity<BaseResponse<?>> test(){
        log.info("log information");

        return ApiResponse.success(SuccessCode.OK);
        // or
        // return ApiResponse.success(SuccessCode.OK, "성공적으로 요청이 완료되었습니다!");
    }

    @GetMapping("/get")
    public ResponseEntity<BaseResponse<?>> find(@RequestParam int id){
        return ApiResponse.success(SuccessCode.OK, testService.find(id));
    }

    @PostMapping("/post/create")
    public ResponseEntity<BaseResponse<?>> create(@Valid @RequestBody TestDto req) {
        return ApiResponse.success(SuccessCode.OK, testService.create(req));
    }

    @GetMapping("/common")
    public String throwCommonException() {
        throw new CommonException(ErrorCode.INVALID_REQUEST, "요청이 잘못되었습니다.");
    }

    @GetMapping("/unknown")
    public String throwUnknownException() {
        throw new RuntimeException("예상치 못한 서버 에러 발생!");
    }
}
