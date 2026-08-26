package com.whereyouad.WhereYouAd.domains.platform.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickAnomalyEventRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickBaselineStatRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformDataCleanupExecutor {

    private static final int BATCH_SIZE = 1000;

    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final ProjectRepository projectRepository;
    private final ClickLogRepository clickLogRepository;
    private final MetricFactRepository metricFactRepository;
    private final ClickAnomalyEventRepository clickAnomalyEventRepository;
    private final ClickBaselineStatRepository clickBaselineStatRepository;

    // 삭제에 영향받는 projectId 수집 (수동 연동 해제 정리 / 회원 탈퇴 스케줄러 등 시스템 내부 호출용)
    @Transactional(readOnly = true)
    public List<Long> collectProjectIds(Long accountId) {
        return adCampaignRepository.findDistinctProjectIdsByPlatformAccountId(accountId);
    }

    // 청크 단위로 ClickAnomalyEvent 삭제 — ad_content_id 가 FK 가 아니라 자동 정리되지 않는다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteClickAnomalyEventChunk(Long platformAccountId) {
        return clickAnomalyEventRepository.deleteByPlatformAccountIdInBatch(platformAccountId, BATCH_SIZE);
    }

    // 청크 단위로 ClickBaselineStat 삭제
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteClickBaselineStatChunk(Long platformAccountId) {
        return clickBaselineStatRepository.deleteByPlatformAccountIdInBatch(platformAccountId, BATCH_SIZE);
    }

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

    // AdCampaign + PlatformConnection + PlatformAccount + 비어있는 Project 원자적 삭제
    @Transactional
    public void deleteAccountAndRelations(Long accountId, List<Long> projectIds) {
        PlatformAccount platformAccount = platformAccountRepository.findById(accountId)
                .orElse(null);

        if (platformAccount == null) {
            log.info("이미 삭제된 PlatformAccount - accountId={}, 정리 skip", accountId);
            return;
        }

        // AdCampaign 제거
        List<AdCampaign> campaigns = adCampaignRepository.findByPlatformAccount(platformAccount);
        if (!campaigns.isEmpty()) {
            adCampaignRepository.deleteAll(campaigns);
            adCampaignRepository.flush();
            // AdCampaign 삭제 시 CascadeType.ALL 로 인해 연관된 AdGroup, AdContent 도 함께 제거됨
        }

        // PlatformConnection 제거
        List<PlatformConnection> connections = platformConnectionRepository.findAllByPlatformAccount_Id(accountId);
        if (!connections.isEmpty()) {
            platformConnectionRepository.deleteAll(connections);
            platformConnectionRepository.flush();
        }

        // 빈 Project 제거
        for (Long projectId : projectIds) {
            if (adCampaignRepository.countByProject_Id(projectId) == 0 &&
                    metricFactRepository.countByProject_Id(projectId) == 0)
            {
                projectRepository.deleteById(projectId);
            }
        }

        // PlatformAccount 제거
        platformAccountRepository.delete(platformAccount);
    }
}