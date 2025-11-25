package dev.woori.wooriLearn.config;

import dev.woori.wooriLearn.domain.account.service.AccountClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

// 외부 서버와 통신할 때 헤더에 appKey / secretKey 붙여서 보내기
@Configuration
public class RestClientConfig {
    private static final String APP_KEY_HEADER = "appKey";
    private static final String SECRET_KEY_HEADER = "secretKey";

    @Value("${external.bank.base-url}")
    private String bankUrl;
    @Value("${spring.env.app-key}")
    private String appKey;
    @Value("${spring.env.secret-key}")
    private String secretKey;

    @Bean
    public AccountClient accountClient() {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(
                        RestClient.builder()
                            .baseUrl(bankUrl)
                            .defaultHeader(APP_KEY_HEADER, appKey)
                            .defaultHeader(SECRET_KEY_HEADER, secretKey)
                            .requestFactory(customRequestFactory())
                            .build()))
                .build()
                .createClient(AccountClient.class);
    }

    ClientHttpRequestFactory customRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5)); // 연결 타임아웃을 5초로 설정
        factory.setReadTimeout(Duration.ofSeconds(5));  // 읽기 타임아웃을 5초로 설정
        return factory;
    }
}
