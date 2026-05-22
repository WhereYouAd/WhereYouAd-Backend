package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformDataCleanupExecutor {

    private static final int BATCH_SIZE = 1000;

    private final ClickLogRepository clickLogRepository;
    private final MetricFactRepository metricFactRepository;

    // 청크 단위로 ClickLog 삭제 — 메인 트랜잭션과 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteClickLogChunk(Long platformAccountId) {
        int deleted = clickLogRepository.deleteByPlatformAccountIdInBatch(platformAccountId, BATCH_SIZE);
        if (deleted > 0) {
            log.debug("ClickLog 청크 삭제 - platformAccountId={}, deleted={}", platformAccountId, deleted);
        }
        return deleted;
    }

    // 청크 단위로 MetricFact 삭제
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteMetricFactChunk(Long platformAccountId) {
        int deleted = metricFactRepository.deleteByPlatformAccountIdInBatch(platformAccountId, BATCH_SIZE);
        if (deleted > 0) {
            log.debug("MetricFact 청크 삭제 - platformAccountId={}, deleted={}", platformAccountId, deleted);
        }
        return deleted;
    }
}