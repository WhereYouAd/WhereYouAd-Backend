package com.whereyouad.WhereYouAd.global.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisUtil {

    private static final DefaultRedisScript<Long> ADVANCE_CLICK_SURGE_STREAK_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            local count = 1
            if current then
                local separator = string.find(current, '|', 1, true)
                if separator then
                    local lastWindow = string.sub(current, 1, separator - 1)
                    local previousCount = tonumber(string.sub(current, separator + 1))
                    if lastWindow == ARGV[1] then
                        count = previousCount or 1
                    elseif lastWindow == ARGV[2] and previousCount then
                        count = previousCount + 1
                    end
                end
            end
            redis.call('SET', KEYS[1], ARGV[1] .. '|' .. count, 'EX', ARGV[3])
            return count
            """, Long.class);

    private static final DefaultRedisScript<Long> DELETE_IF_VALUE_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> RENEW_IF_VALUE_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);

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

    /**
     * 클릭 급증 streak 상태를 {@code lastDetectedWindow|count} 형식으로 원자적으로 갱신한다.
     * 같은 윈도우 재실행은 멱등 처리하고, 직전 윈도우일 때만 횟수를 증가시킨다.
     */
    public Long advanceClickSurgeStreak(String key, String currentWindow, String previousWindow, long durationSeconds) {
        return template.execute(
                ADVANCE_CLICK_SURGE_STREAK_SCRIPT,
                Collections.singletonList(key),
                currentWindow,
                previousWindow,
                String.valueOf(durationSeconds));
    }

    /**
     * 키 값이 아직 {@code expectedValue}와 같을 때만 삭제한다.
     */
    public boolean deleteIfValueMatches(String key, String expectedValue) {
        Long deleted = template.execute(
                DELETE_IF_VALUE_MATCHES_SCRIPT,
                Collections.singletonList(key),
                expectedValue);
        return deleted != null && deleted > 0;
    }

    /**
     * 키 값이 아직 {@code expectedValue}와 같을 때만 TTL을 갱신한다.
     */
    public boolean renewIfValueMatches(String key, String expectedValue, long durationSeconds) {
        Long renewed = template.execute(
                RENEW_IF_VALUE_MATCHES_SCRIPT,
                Collections.singletonList(key),
                expectedValue,
                String.valueOf(durationSeconds));
        return renewed != null && renewed > 0;
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

    // 분산 락 해제용: 저장된 값이 기대값과 일치할 때만 삭제 (Lua로 원자적 처리, 남의 락 삭제 방지)
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    public Boolean compareAndDelete(String key, String expectedValue) {
        Long result = template.execute(COMPARE_AND_DELETE_SCRIPT, List.of(key), expectedValue);
        return result != null && result == 1L;
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

    // Set 에 멤버 추가 + TTL 갱신
    public Long sAddExpire(String key, long durationSeconds, String... members) {
        Long added = template.opsForSet().add(key, members);
        template.expire(key, Duration.ofSeconds(durationSeconds));
        return added;
    }

    // Set 전체 멤버 조회
    public Set<String> sMembers(String key) {
        Set<String> members = template.opsForSet().members(key);
        return members != null ? members : Collections.emptySet();
    }

    // 다건 GET (없는 키는 null 요소로 반환)
    public List<String> multiGetData(List<String> keys) {
        List<String> values = template.opsForValue().multiGet(keys);
        return values != null ? values : Collections.emptyList();
    }

}
