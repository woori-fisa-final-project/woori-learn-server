package dev.woori.wooriLearn.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import dev.woori.wooriLearn.domain.account.entity.AccountSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 캐시 설정
 * - 자동이체 목록 조회 성능 향상
 * - TTL: 5분 (자주 변경되지 않는 데이터)
 * - RedisConnectionFactory는 Spring Boot 자동 설정 사용 (application.yml 기반)
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper objectMapper) {
        // 형식 설정: key = String, value = object
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 직렬화(객체->json) 시 클래스 타입 정보를 JSON에 포함시키도록 설정 => 안전하게 역직렬화(json->객체) 가능
        // NON_FINAL: final이 아닌 모든 클래스에 대해 타입 정보 포함
        // JsonTypeInfo.As.PROPERTY: JSON에 타입 정보 삽입
        ObjectMapper customObjectMapper = objectMapper.copy();
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(AccountSession.class) // 역직렬화 허용 클래스 목록
                .build();
        customObjectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        template.setConnectionFactory(redisConnectionFactory());

        // Key: 문자열
        template.setKeySerializer(new StringRedisSerializer());

        // Value: JSON 형태로 저장
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(customObjectMapper));

        return template;
    }

    /**
     * 캐시 매니저 설정
     * - Spring Boot가 자동 설정한 RedisConnectionFactory와 ObjectMapper 사용
     * - ObjectMapper는 Spring 컨테이너가 관리하는 빈을 주입받아 일관된 설정 유지
     */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        // Redis 직렬화 설정
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // 5분 TTL
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
