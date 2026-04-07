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

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaUpsertService {

    /**
     * Meta 마케팅 API로부터 수집된 광고 데이터를 실제 내부 DB에 갱신/저장(Upsert)하는 UPSERT 전담 Service
     *
     * - 각 메서드 단위로 짧고 독립적인 트랜잭션(REQUIRES_NEW) 보장
     * - 외부 API 통신이 지연될 때 DB 커넥션을 오래 물고 있는 현상(Connection Pool Exhaustion) 방지 위해 MetaSyncService와 분리
     */

    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final AdContentRepository adContentRepository;
    private final MetricFactRepository metricFactRepository;

    //Meta Campaign -> AdCampaign UPSERT
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdCampaign upsertCampaign(MetaDTO.Campaign src, Organization org, PlatformAccount platformAccount) {
        AdCampaign newData = MetaConverter.toCampaign(src, org, platformAccount);
        return adCampaignRepository
                .findByExternalCampaignIdAndProvider(src.id(), Provider.META)
                .map(existing -> {
                    existing.updateFromApi(
                            newData.getName(),
                            newData.getStatus(),
                            newData.getBudget(),
                            newData.getGoal(),
                            newData.getStartDate(),
                            newData.getEndDate()
                    );
                    return adCampaignRepository.save(existing);
                })
                .orElseGet(() -> adCampaignRepository.save(newData));
    }

    //Meta AdSet -> AdGroup UPSERT
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdGroup upsertAdGroup(MetaDTO.AdSet src, AdCampaign campaign) {
        AdGroup newData = MetaConverter.toAdGroup(src, campaign);
        return adGroupRepository
                .findByExternalGroupId(src.id())
                .map(existing -> {
                    existing.updateFromApi(
                            newData.getName(),
                            newData.getStatus(),
                            newData.getTargetingInfo());
                    return adGroupRepository.save(existing);
                })
                .orElseGet(() -> adGroupRepository.save(newData));
    }

    //Meta Ad -> AdContent UPSERT
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdContent upsertAdContent(MetaDTO.Ad src, AdGroup adGroup) {
        AdContent newData = MetaConverter.toAdContent(src, adGroup);
        return adContentRepository
                .findByExternalAdId(src.id())
                .map(existing -> {
                    existing.updateFromApi(
                            newData.getName(),
                            newData.getType(),
                            newData.getStatus(),
                            newData.getDescription());
                    return adContentRepository.save(existing);
                })
                .orElseGet(() -> adContentRepository.save(newData));
    }

    //Meta Insights -> AdContent UPSERT
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertMetricFact(MetaDTO.Insight src, AdContent adContent, AdCampaign adCampaign, PlatformAccount platformAccount) {
        MetricFact newData = MetaConverter.toMetricFact(src, adContent, adCampaign, platformAccount);
        metricFactRepository
                .findByAdContentAndTimeBucketAndProvider(
                        adContent, newData.getTimeBucket(), Provider.META)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateFromApi(
                                    newData.getImpressions(),
                                    newData.getClicks(),
                                    newData.getConversions(),
                                    newData.getSpend(),
                                    newData.getRevenue());
                            metricFactRepository.save(existing);
                        },
                        () -> metricFactRepository.save(newData)
                );
    }
}

