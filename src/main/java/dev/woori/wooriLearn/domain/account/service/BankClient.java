package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.BankTokenReqDto;
import dev.woori.wooriLearn.domain.account.dto.BankTokenResDto;
import dev.woori.wooriLearn.domain.account.dto.TokenData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankClient {
    private final RestTemplate restTemplate;
    private static final String APP_KEY_HEADER = "appKey";
    private static final String SECRET_KEY_HEADER = "secretKey";

    @Value("${external.bank.base-url}")
    private String bankUrl;
    @Value("${spring.env.app-key}")
    private String appKey;
    @Value("${spring.env.secret-key}")
    private String secretKey;

    public TokenData requestBankToken(String userId) {
        // url 지정
        String url = UriComponentsBuilder.fromHttpUrl(bankUrl).path("/auth/token").toUriString();

        // 헤더 지정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(APP_KEY_HEADER, appKey);
        headers.add(SECRET_KEY_HEADER, secretKey);

        // body 지정
        BankTokenReqDto payload = BankTokenReqDto.builder().userId(userId).build();

        // header + body를 모아 HttpEntity로 지정
        HttpEntity<BankTokenReqDto> entity = new HttpEntity<>(payload, headers);

        try {
            // post 방식으로 요청을 보내기 (url/엔티티/응답 dto)
            ResponseEntity<BankTokenResDto> response = restTemplate
                    .postForEntity(url, entity, BankTokenResDto.class);

            BankTokenResDto body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                log.error("인증서버 응답 오류: status={}, body={}", response.getStatusCode(), body);
                throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "인증서버 응답이 올바르지 않습니다.");
            }
            return body.data();
        } catch (RestClientException e) {
            log.error("인증서버 통신 오류: {}", e.getMessage());
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "인증서버와 통신할 수 없습니다.");
        }
    }
}
