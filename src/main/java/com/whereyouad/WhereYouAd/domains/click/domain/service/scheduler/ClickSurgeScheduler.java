package com.whereyouad.WhereYouAd.domains.click.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.click.domain.config.ClickSurgeProperties;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickSurgeDetectionService;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickSurgeScheduler {

    private final RedisUtil redisUtil;
    private final ClickSurgeDetectionService clickSurgeDetectionService;
    private final ClickSurgeProperties properties;

    private static final String LOCK_KEY_PREFIX = "lock:scheduler:click-surge:";
    private static final long LOCK_TTL_SECONDS = 290; // 다음 실행 전 자연 만료

    // +10초 오프셋 - Kafka 컨슈머 lag 정착 대기 후 직전 마감 윈도우 처리
    @Scheduled(cron = "10 0/5 * * * *", zone = "Asia/Seoul")
    public void runSurgeDetection() {
        if (!properties.isEnabled()) {
            return;
        }

        // 직전에 마감된 5분 윈도우 (ClickConsumer와 동일 clock 사용 필수)
        LocalDateTime windowStart = ClickWindowKeys
                .floorToWindowStart(LocalDateTime.now(ClickWindowKeys.ZONE_ID), properties.getWindowMinutes())
                .minusMinutes(properties.getWindowMinutes());

        // 다중 인스턴스 대비 리더락 - 윈도우당 정확히 1회 처리
        String lockKey = LOCK_KEY_PREFIX + windowStart.format(ClickWindowKeys.MINUTE_FORMATTER);
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS))) {
            log.debug("[급증감지] 다른 인스턴스가 처리 중, skip: windowStart={}", windowStart);
            return;
        }

        try {
            clickSurgeDetectionService.detectForWindow(windowStart);
        } catch (Exception e) {
            log.error("[급증감지] 윈도우 처리 실패: windowStart={}", windowStart, e);
        }
    }
}
