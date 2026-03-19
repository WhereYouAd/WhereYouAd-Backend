package com.whereyouad.WhereYouAd.global.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate template;

    //Redis 에 데이터 저장(유효시간 설정)
    public void setDataExpire(String key, String value, long duration) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        Duration expireDuration = Duration.ofSeconds(duration);
        valueOperations.set(key, value, expireDuration);
    }

    //Redis 에서 데이터 꺼내기(Value 꺼내기)
    public String getData(String key) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        return valueOperations.get(key);
    }

    //Redis 에서 데이터 지우기(key 값 기반)
    public void deleteData(String key) {
        template.delete(key);
    }

    // (클릭수) 1 증가
    public Long incrementDataExpire(String key, long durationSeconds) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        Long count = valueOperations.increment(key);

        // 클릭수 정보 새로 생성 됐을 시 만료 정보 설정
        if (count != null && count == 1L)
            template.expire(key, Duration.ofSeconds(durationSeconds));

        return count;
    }
}
