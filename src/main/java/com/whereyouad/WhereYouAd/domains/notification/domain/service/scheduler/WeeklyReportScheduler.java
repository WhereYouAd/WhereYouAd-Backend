package com.whereyouad.WhereYouAd.domains.notification.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.notification.domain.service.WeeklyReportNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final WeeklyReportNotificationService weeklyReportNotificationService;

    // 매주 월요일 오전 8시 (Asia/Seoul 기준)
    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Seoul")
    public void triggerWeeklyReport() {
        log.info("[WeeklyReportScheduler] 주간 광고 리포트 발송 시작");
        weeklyReportNotificationService.sendWeeklyReports();
    }
}
