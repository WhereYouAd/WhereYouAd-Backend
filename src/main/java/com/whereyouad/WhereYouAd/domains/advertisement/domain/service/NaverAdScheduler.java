package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverAdScheduler {

    private final NaverAdSyncService naverAdSyncService;
    private final PlatformConnectionRepository platformConnectionRepository;

    // 매일 새벽 2시 메타데이터(광고 정보) 동기화
    @Scheduled(cron = "0 0 2 * * *")
    public void syncNaverAdStats() {
        log.info("NAVER 통계 동기화 스케줄러 시작");

        // 현재 DB에 저장된 모든 NAVER 연결 계정을 가져옴
        List<PlatformConnection> connections = platformConnectionRepository.findByPlatformAccount_Provider(Provider.NAVER);

        // NAVER 계정 (PlatformConnection) 단위로 메타데이터 및 통계 동기화
        for (PlatformConnection conn : connections) {
            Long connectionId = conn.getId();
            log.info("NAVER 스케줄러 처리 중 - connectionId: {}", connectionId);

            try {
                // 광고 정보 동기화 서비스 실행 (DB에 upsert, Metric_fact는 아래에서)
                naverAdSyncService.syncAllMetadata(connectionId);
            } catch (Exception e) {
                log.error("connectionId: {} 메타데이터 동기화 실패: {}", connectionId, e.getMessage(), e);
            }
        }

        log.info("NAVER 통계 동기화 스케줄러 종료");
    }
}
