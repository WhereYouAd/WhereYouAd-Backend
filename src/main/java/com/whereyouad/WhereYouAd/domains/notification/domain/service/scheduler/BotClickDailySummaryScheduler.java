package com.whereyouad.WhereYouAd.domains.notification.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.BotClickSummaryNotificationService;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotClickDailySummaryScheduler {

    private final BotClickSummaryNotificationService botClickSummaryNotificationService;
    private final RedisUtil redisUtil;

    @Value("${click.bot-summary.enabled:true}")
    private boolean enabled;

    private static final String LOCK_KEY_PREFIX = "lock:scheduler:bot-summary:";
    private static final long LOCK_TTL_SECONDS = 3600;
    private static final DateTimeFormatter DATE_KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void triggerDailyBotSummary() {
        if (!enabled) {
            return;
        }

        // 다중 인스턴스 대비 리더락 - 날짜 스탬프로 하루 정확히 1회 발송 보장
        String lockKey = LOCK_KEY_PREFIX + LocalDate.now(ClickWindowKeys.ZONE_ID).format(DATE_KEY_FMT);
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS))) {
            log.debug("[봇클릭요약] 다른 인스턴스가 처리 중, skip");
            return;
        }

        botClickSummaryNotificationService.sendDailyBotSummaries();
    }
}
