package com.whereyouad.WhereYouAd.global.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

    // 분산 락용: 키가 없을 때만 set. Meta 광고 데이터 갱신 요청시 과도한 갱신 버튼 연타로 인한 Meta API 차단 방지용
    public Boolean setIfAbsent(String key, String value, long durationSeconds) {
        return template.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(durationSeconds));
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

    // List 데이터 저장 (왼쪽 끝에 저장)
    public Long leftPush(String key, String value) {
        return template.opsForList().leftPush(key, value);
    }

    // List 데이터 꺼내기 (오른쪽 끝에서 추출 및 제거)
    public String rightPop(String key) {
        return template.opsForList().rightPop(key);
    }

    // 카운터
    public Long increment(String key) {
        return template.opsForValue().increment(key);
    }

    // 특정 키의 만료 시간 설정
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return template.expire(key, timeout, unit);
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
