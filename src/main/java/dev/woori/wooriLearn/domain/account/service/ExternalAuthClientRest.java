package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.ExternalAuthReqDto;
import dev.woori.wooriLearn.domain.account.dto.ExternalAuthResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 외부 인증 서버에 OTP 발급을 요청하는 REST 클라이언트 구현체.
 * <p>
 * 동작 개요
 * - requestOtp(name, birthdate, phoneDigits)를 호출하면
 * 1) {name, birthdate, phone} JSON 페이로드를 만들고
 * 2) 외부 인증 서버로 POST 요청을 보낸 뒤
 * 3) 응답 JSON에서 "code" 필드를 추출하여 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAuthClientRest implements ExternalAuthClient {

    /**
     * 외부 호출용 RestTemplate Bean
     */
    private final RestTemplate externalAuthRestTemplate;

    /**
     * 인증 서버 베이스 URL
     */
    @Value("${account.external-auth.base-url}")
    private String baseUrl;

    /**
     * 인증 서버 요청 경로
     */
    @Value("${account.external-auth.request-path:/otp}")
    private String requestPath;

    @Override
    public String requestOtp(String name, String birthdate, String phoneNum) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl).path(requestPath).toUriString();

        ExternalAuthReqDto payload = new ExternalAuthReqDto(name, birthdate, phoneNum);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ExternalAuthReqDto> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<ExternalAuthResDto> resp = externalAuthRestTemplate
                    .postForEntity(url, entity, ExternalAuthResDto.class);

            ExternalAuthResDto body = resp.getBody();
            if (!resp.getStatusCode().is2xxSuccessful() || body == null || body.code() == null || body.code().isBlank()) {
                log.error("인증서버 응답 오류: status={}, body={}", resp.getStatusCode(), body);
                throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "인증서버 응답이 올바르지 않습니다.");
            }
            return body.code();
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("인증서버 통신 오류: {}", e.getMessage());
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "인증서버와 통신할 수 없습니다.");
        }
    }
}