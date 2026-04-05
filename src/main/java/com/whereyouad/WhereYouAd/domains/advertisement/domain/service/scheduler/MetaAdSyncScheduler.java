package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.MetaAdApiService;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetaAdSyncScheduler {

    private final MetaAdApiService metaAdApiService;
    private final PlatformConnectionRepository connectionRepository;

    /**
     * 매일 새벽 2시에 실행
     * 모든 Meta 연동 계정의 광고 데이터를 UPSERT 동기화
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void scheduledSync() {
        log.info("[META SCHEDULER] 자동 동기화 시작");

        // 어제~오늘 (최근 1일 데이터 갱신)
        String startDate = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 모든 META PlatformConnection 기반 조직 Id 리스트 조회
        List<Long> targetOrgIds = connectionRepository.findOrganizationIdsByProvider(Provider.META);

        int successCount = 0;
        int failCount = 0;

        for (Long orgId : targetOrgIds) {
            try {

                MetaResponse.MetaSyncSummary response = metaAdApiService.syncAll(orgId, startDate, endDate);
                log.info("[META SCHEDULER] orgId={} 동기화 성공 - adCampaign={}, adGroup={}, adContent={}, metricFact{}",
                        orgId,
                        response.adCampaignCount(),
                        response.adGroupCount(),
                        response.adContentCount(),
                        response.metricCount()
                );

                successCount++;

            } catch (Exception e) {

                log.error("[META SCHEDULER] orgId={} 동기화 실패", orgId, e);
                failCount++;
            }

            // Meta API Rate Limit 차단 방지 코드
            // ==========================================================
            // 한 조직의 작업을 마친 후 성공/실패와 무관하게 시스템을 잠시 대기
            // 계정 루핑이 너무 빨리 돌아 매타 서버가 어뷰징이나 매크로로 간주하고
            // IP 대역이나 앱 전체의 통신을 일괄 차단하는 것을 방지 (1초 대기)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // JVM 스레드 종료 요청이 들어올 경우 강제로 삼키지 않고 인터럽트 플래그를 재생성하여 안전 종료 유도
                Thread.currentThread().interrupt();
                log.warn("[META SCHEDULER] 동기화 지연 로직 대기 중 Thread 인터럽트 발생");
                break; // 스케줄러 강제 중단
            }
        }

        log.info("[META SCHEDULER] 자동 동기화 완료 - 성공:{}, 실패:{}, 총:{}",
                successCount, failCount, targetOrgIds.size());
    }
}
