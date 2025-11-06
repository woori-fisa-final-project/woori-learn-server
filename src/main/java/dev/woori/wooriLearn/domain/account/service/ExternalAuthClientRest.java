package dev.woori.wooriLearn.domain.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 외부 인증 서버에 OTP 발급을 요청하는 REST 클라이언트 구현체.
 *
 * 동작 개요
 * - requestOtp(name, birthdate, phoneDigits)를 호출하면
 *   1) {name, birthdate, phone} JSON 페이로드를 만들고
 *   2) 외부 인증 서버로 POST 요청을 보낸 뒤
 *   3) 응답 JSON에서 "code" 필드를 추출하여 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAuthClientRest implements ExternalAuthClient {

    /** 외부 호출용 RestTemplate Bean */
    private final RestTemplate externalAuthRestTemplate;

    /** 인증 서버 베이스 URL */
    @Value("${account.external-auth.base-url}")
    private String baseUrl;

    /** 인증 서버 요청 경로 */
    @Value("${account.external-auth.request-path:/otp}")
    private String requestPath;

    @Override
    public String requestOtp(String name, String birthdate, String phoneNum) {
        String url = baseUrl + requestPath;

        // 전송 페이로드(JSON)
        Map<String, Object> payload = Map.of(
                "name", name,
                "birthdate", birthdate,
                "phone", phoneNum
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // POST 전송
        ResponseEntity<Map> resp = externalAuthRestTemplate
                .postForEntity(url, new HttpEntity<>(payload, headers), Map.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || !resp.getBody().containsKey("code")) {
            log.error("인증서버 응답 오류: status={}, body={}", resp.getStatusCode(), resp.getBody());
            throw new IllegalStateException("인증서버 응답 오류");
        }
        return String.valueOf(resp.getBody().get("code"));
    }
}