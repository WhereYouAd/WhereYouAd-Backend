package com.whereyouad.WhereYouAd.domains.notification.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.PushNotificationEventProducer;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.push.BrowserPushDataAccess;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// 웹 푸시 실패 delivery 재큐잉.
// - status=FAILED AND retryCount < max 만 대상
// - 만료 응답(410/404) 로 실패한 subscription 은 DataAccess.recordResults 에서 이미 삭제됐으므로,
//   재시도 시 살아있는 다른 구독으로만 재발송된다 (계속 실패하는 endpoint 를 반복 두드리지 않음).
@Slf4j
@Component
@RequiredArgsConstructor
public class PushRetryScheduler {

    private final BrowserPushDataAccess dataAccess;
    private final PushNotificationEventProducer pushNotificationEventProducer;
    private final RedisUtil redisUtil;

    @Value("${web-push.retry.max-count:3}")
    private int maxRetryCount;

    @Value("${web-push.retry.batch-size:20}")
    private int batchSize;

    private static final String LOCK_KEY = "lock:scheduler:push-retry";
    private static final long LOCK_TTL_SECONDS = 240;
    private static final int MAX_BATCH_SIZE = 100;

    // application.yml 의 web-push.retry.interval-minutes 와 맞춰 5분(300초)마다 실행
    @Scheduled(fixedDelayString = "${web-push.retry.interval-minutes:5}", timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void retryFailed() {
        String ownershipToken = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(LOCK_KEY, ownershipToken, LOCK_TTL_SECONDS))) {
            log.debug("[웹푸시 재시도] 다른 인스턴스가 처리 중, skip");
            return;
        }

        try {
            int effectiveBatchSize = Math.max(1, Math.min(batchSize, MAX_BATCH_SIZE));
            List<PushNotificationEvent> retryEvents = dataAccess.loadRetryEvents(maxRetryCount, effectiveBatchSize);
            if (retryEvents.isEmpty()) {
                return;
            }
            if (!redisUtil.renewIfValueMatches(LOCK_KEY, ownershipToken, LOCK_TTL_SECONDS)) {
                log.warn("[웹푸시 재시도] 조회 중 락 소유권 상실, 발행 중단");
                return;
            }

            long renewAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(LOCK_TTL_SECONDS / 2);
            log.info("[웹푸시 재시도] 재큐잉 대상 알림 수={}", retryEvents.size());
            for (PushNotificationEvent event : retryEvents) {
                if (System.nanoTime() >= renewAt) {
                    if (!redisUtil.renewIfValueMatches(LOCK_KEY, ownershipToken, LOCK_TTL_SECONDS)) {
                        log.warn("[웹푸시 재시도] 발행 중 락 소유권 상실, 남은 작업 중단");
                        return;
                    }
                    renewAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(LOCK_TTL_SECONDS / 2);
                }
                try {
                    pushNotificationEventProducer.produce(event);
                } catch (Exception e) {
                    log.error("[웹푸시 재시도] Kafka 재발행 실패 notificationId={}", event.getNotificationId(), e);
                }
            }
        } finally {
            redisUtil.deleteIfValueMatches(LOCK_KEY, ownershipToken);
        }
    }
}
