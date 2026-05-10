package com.whereyouad.WhereYouAd.domains.advertisement.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;

import java.util.List;
import java.time.LocalDateTime;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;

public class AdvertisementConverter {

    public static AdvertisementResponse.AdContentInfoResponse toAdContentInfo(AdContent adContent,
            AdvertisementResponse.AdGroupInfoResponse adGroupInfoResponse) {
        Provider provider = adContent.getAdGroup().getAdCampaign().getProvider();
        return new AdvertisementResponse.AdContentInfoResponse(
                adContent.getId(), adContent.getName(), provider,
                adContent.getTrackingUrl(), adContent.getLandingUrl(),
                adContent.getDescription(), adContent.getStatus(), adGroupInfoResponse.targetInfo());
    }

    public static AdvertisementResponse.AdContentInfosResponse toAdContentsInfo(List<AdContent> adContents) {
        return new AdvertisementResponse.AdContentInfosResponse(adContents.stream().map(
                adContent -> toAdContentInfo(adContent, AdvertisementConverter.toAdGroupInfo(adContent.getAdGroup())))
                .toList());
    }

    public static AdvertisementResponse.AdGroupInfoResponse toAdGroupInfo(AdGroup adGroup) {
        return new AdvertisementResponse.AdGroupInfoResponse(
                adGroup.getId(), adGroup.getName(), adGroup.getTargetingInfo(), adGroup.getStatus());
    }

    public static AdvertisementResponse.AdCampaignSimpleResponse toAdCampaignSimple(AdCampaign adCampaign) {
        return new AdvertisementResponse.AdCampaignSimpleResponse(
                adCampaign.getId(), adCampaign.getName(), adCampaign.getDescription());
    }

    public static AdvertisementResponse.AdCampaignListResponse toAdCampaignList(List<AdvertisementResponse.AdCampaignSimpleResponse> simpleResponses) {
        return new AdvertisementResponse.AdCampaignListResponse(simpleResponses);
    }

    public static MetricFact createMetricFact(AdContent adContent, LocalDateTime timeBucket, Grain grain, Provider provider) {
        Project project = null;
        AdCampaign adCampaign = null;
        if (adContent.getAdGroup() != null && adContent.getAdGroup().getAdCampaign() != null) {
            adCampaign = adContent.getAdGroup().getAdCampaign();
            project = adCampaign.getProject();
        }

        return MetricFact.builder()
                .grain(grain)
                .timeBucket(timeBucket)
                .provider(provider)
                .adContent(adContent)
                .adCampaign(adCampaign)
                .platformAccount(adCampaign != null ? adCampaign.getPlatformAccount() : null)
                .project(project)
                .conversions(null)
                .revenue(null)
                .build();
    }
}
