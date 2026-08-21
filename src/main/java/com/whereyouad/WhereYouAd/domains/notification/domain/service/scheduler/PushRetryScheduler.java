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

    private static final String LOCK_KEY = "lock:scheduler:push-retry";
    private static final long LOCK_TTL_SECONDS = 240;

    // application.yml 의 web-push.retry.interval-minutes 와 맞춰 5분(300초)마다 실행
    @Scheduled(fixedDelayString = "${web-push.retry.interval-minutes:5}", timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void retryFailed() {
        // 다중 인스턴스 대비 리더락 - 락 TTL 은 스케줄 간격보다 살짝 짧게
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(LOCK_KEY, "1", LOCK_TTL_SECONDS))) {
            log.debug("[웹푸시 재시도] 다른 인스턴스가 처리 중, skip");
            return;
        }

        List<PushNotificationEvent> retryEvents = dataAccess.loadRetryEvents(maxRetryCount);
        if (retryEvents.isEmpty()) {
            return;
        }

        log.info("[웹푸시 재시도] 재큐잉 대상 알림 수={}", retryEvents.size());
        for (PushNotificationEvent event : retryEvents) {
            try {
                pushNotificationEventProducer.produce(event);
            } catch (Exception e) {
                log.error("[웹푸시 재시도] Kafka 재발행 실패 notificationId={}", event.getNotificationId(), e);
            }
        }
    }
}
