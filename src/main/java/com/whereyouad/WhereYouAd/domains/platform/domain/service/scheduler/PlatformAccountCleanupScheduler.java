package com.whereyouad.WhereYouAd.domains.platform.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.service.PlatformService;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAccountCleanupScheduler {

    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformService platformService;

    // 수동 연동 해제(DISCONNECTED 마킹)된 PlatformAccount 의 실제 데이터 정리
    // cleanupAccount 는 ClickLog/MetricFact/AdCampaign/Connection 먼저 삭제, PlatformAccount는 맨 마지막에 삭제
    // TODO : 스케줄러 주기를 어떻게 할지? (현재는 새벽 4시에 실행)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupDeletedAccounts() {
        List<Long> accountIds = platformAccountRepository.findIdsByStatus(PlatformStatus.DISCONNECTED);
        if (accountIds.isEmpty()) {
            return;
        }
        log.info("연동 해제(DISCONNECTED) 계정 정리 스케줄러 실행 - 대상 {}건", accountIds.size());

        int successCount = 0;
        for (Long accountId : accountIds) {
            try {
                platformService.disconnectAccountBySystem(accountId);
                successCount++;
            } catch (Exception e) {
                // 한 계정의 실패가 나머지 계정 정리를 막지 않도록 격리 → 다음 회차에 재시도
                log.error("연동 해제 계정 정리 실패 - accountId={}", accountId, e);
            }
        }

        log.info("연동 해제(DISCONNECTED) 계정 정리 완료 - 대상 갯수: {}, 성공 갯수: {}", accountIds.size(), successCount);
    }
}