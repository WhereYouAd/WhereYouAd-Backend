package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.converter.MetaConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaUpsertService {

    /**
     * Meta 마케팅 API로부터 수집된 광고 데이터를 실제 내부 DB에 갱신/저장(Upsert)하는 UPSERT 전담 Service
     *
     * - 페이지 단위 배치 트랜잭션(REQUIRES_NEW): 한 페이지의 항목을 하나의 트랜잭션으로 처리
     *   → 항목별 개별 트랜잭션 대비 커넥션 소비 및 커밋 오버헤드를 대폭 감소
     * - 외부 API 통신이 지연될 때 DB 커넥션을 오래 물고 있는 현상(Connection Pool Exhaustion) 방지 위해 MetaSyncService와 분리
     * - 모든 조회 쿼리에 부모 엔티티 조건 포함 → 서로 다른 Meta 광고 계정 간 외부 ID 충돌 방지
     */

    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final AdContentRepository adContentRepository;
    private final MetricFactRepository metricFactRepository;

    // Meta Campaign 페이지 -> AdCampaign 배치 UPSERT (externalCampaignId + Provider 스코프)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, AdCampaign> upsertCampaigns(List<MetaDTO.Campaign> campaigns, Organization org, PlatformAccount platformAccount) {
        Map<String, AdCampaign> result = new HashMap<>();
        for (MetaDTO.Campaign src : campaigns) {
            AdCampaign newData = MetaConverter.toCampaign(src, org, platformAccount);
            AdCampaign saved = adCampaignRepository
                    .findByExternalCampaignIdAndPlatformAccount(src.id(), platformAccount)
                    .map(existing -> {
                        existing.update(
                                newData.getName(),
                                newData.getStatus(),
                                newData.getBudget(),
                                newData.getGoal(),
                                newData.getStartDate(),
                                newData.getEndDate(),
                                newData.getDescription()
                        );
                        return adCampaignRepository.save(existing);
                    })
                    .orElseGet(() -> adCampaignRepository.save(newData));
            result.put(src.id(), saved);
        }
        return result;
    }

    // Meta AdSet 페이지 -> AdGroup 배치 UPSERT (externalGroupId + 부모 Campaign 스코프)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, AdGroup> upsertAdGroups(List<MetaDTO.AdSet> adSets, Map<String, AdCampaign> campaignMap, PlatformAccount platformAccount) {
        Map<String, AdGroup> result = new HashMap<>();
        for (MetaDTO.AdSet src : adSets) {
            AdCampaign parentCampaign = campaignMap.get(src.campaignId());
            if (parentCampaign == null) continue;
            AdGroup newData = MetaConverter.toAdGroup(src, parentCampaign, platformAccount);
            AdGroup saved = adGroupRepository
                    .findByExternalGroupIdAndAdCampaign(src.id(), parentCampaign)
                    .map(existing -> {
                        existing.update(
                                newData.getName(),
                                newData.getStatus(),
                                newData.getTargetingInfo());
                        existing.replaceBudget(newData.getBudget());
                        return adGroupRepository.save(existing);
                    })
                    .orElseGet(() -> adGroupRepository.save(newData));
            result.put(src.id(), saved);
        }
        return result;
    }

    // Meta Ad 페이지 -> AdContent 배치 UPSERT (externalAdId + 부모 AdGroup 스코프)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, AdContent> upsertAdContents(List<MetaDTO.Ad> ads, Map<String, AdGroup> adSetMap) {
        Map<String, AdContent> result = new HashMap<>();
        for (MetaDTO.Ad src : ads) {
            AdGroup parentAdGroup = adSetMap.get(src.adSetId());
            if (parentAdGroup == null) continue;
            AdContent newData = MetaConverter.toAdContent(src, parentAdGroup);

            AdContent saved = adContentRepository
                    .findByExternalAdIdAndAdGroup(src.id(), parentAdGroup)
                    .map(existing -> {
                        existing.update(
                                newData.getName(),
                                newData.getType(),
                                newData.getStatus(),
                                newData.getDescription(),
                                newData.getTrackingUrl(),
                                newData.getLandingUrl());
                        return adContentRepository.save(existing);
                    })
                    .orElseGet(() -> adContentRepository.save(newData));

            result.put(src.id(), saved);
        }
        return result;
    }

    // Meta Insight 페이지 -> MetricFact 배치 UPSERT (adContent + timeBucket + Provider 스코프)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int upsertMetricFacts(List<MetaDTO.Insight> insights, Map<String, AdContent> adMap,
                                 Map<String, AdCampaign> campaignMap, PlatformAccount platformAccount) {
        int count = 0;
        for (MetaDTO.Insight src : insights) {
            AdContent adContent = adMap.get(src.adId());
            AdCampaign adCampaign = campaignMap.get(src.campaignId());
            if (adContent == null || adCampaign == null) continue;
            MetricFact newData = MetaConverter.toMetricFact(src, adContent, adCampaign, platformAccount);
            metricFactRepository
                    .findByAdContentAndTimeBucketAndProvider(adContent, newData.getTimeBucket(), Provider.META)
                    .ifPresentOrElse(
                            existing -> {
                                existing.update(
                                        newData.getImpressions(),
                                        newData.getClicks(),
                                        newData.getConversions(),
                                        newData.getSpend(),
                                        newData.getRevenue());
                                metricFactRepository.save(existing);
                            },
                            () -> metricFactRepository.save(newData)
                    );
            count++;
        }
        return count;
    }
}

