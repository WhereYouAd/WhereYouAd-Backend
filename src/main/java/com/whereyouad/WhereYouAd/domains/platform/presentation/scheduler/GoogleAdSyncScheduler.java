package com.whereyouad.WhereYouAd.domains.platform.presentation.scheduler;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google.GoogleAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleAdSyncScheduler {

    private final GoogleAdService googleAdService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySync() {
        log.info("[스케줄러 시작] 구글 광고 데이터 새벽 동기화 배치 실행");

        try {
            googleAdService.syncAllGoogleAdsData();
            log.info("[스케줄러 완료] 구글 광고 데이터 동기화 성공");
        } catch (Exception e) {
            log.error("[스케줄러 실패] 구글 광고 데이터 동기화 중 에러 발생", e);
        }
    }
}
