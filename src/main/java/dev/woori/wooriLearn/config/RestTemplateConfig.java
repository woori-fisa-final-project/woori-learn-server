package dev.woori.wooriLearn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 인증 서버와 통신할 때 사용
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 외부 인증 서버 호출용 RestTemplate Bean
     * @param connectTimeout 소켓 연결을 맺을 때까지 기다리는 최대 시간(ms)
     * @param readTimeout   연결이 성사된 이후, 응답 바디의 첫 바이트를 기다리는 최대 시간(ms)
     *                      (서버가 느리거나 무응답일 때 읽기 시도 중단)
     * @return              타임아웃이 설정된 RestTemplate
     */
    @Bean
    public RestTemplate externalAuthRestTemplate(
            @Value("${account.external-auth.connect-timeout-ms:2000}") int connectTimeout,
            @Value("${account.external-auth.read-timeout-ms:3000}") int readTimeout) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connectTimeout);
        f.setReadTimeout(readTimeout);
        return new RestTemplate(f);
    }
}