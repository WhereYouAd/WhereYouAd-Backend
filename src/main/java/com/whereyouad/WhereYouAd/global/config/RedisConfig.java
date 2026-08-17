package com.whereyouad.WhereYouAd.global.config;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    // Lettuce 커맨드(GET/SET/EXPIRE 등)별 레이턴시를 Micrometer로 수집
    // -> lettuce_command_completion_seconds{command="..."} 메트릭 노출
    @Bean
    public ClientResourcesBuilderCustomizer lettuceMetricsCustomizer(MeterRegistry meterRegistry) {
        return builder -> builder.commandLatencyRecorder(
                new MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.create())
        );
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Redis 캐시 설정 정의
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // Key 직렬화: String ("user:profile::1" 처럼 보기 좋게 저장)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value 직렬화: JSON (자바 객체 -> JSON 변환 저장)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // TTL 설정: 데이터 유효 시간 (30분)
                .entryTtl(Duration.ofMinutes(30))
                // null 데이터는 캐싱하지 않음
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
