package com.whereyouad.WhereYouAd.domains.advertisement.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;

import java.util.List;

public class AdvertisementConverter {

    public static AdvertisementResponse.AdContentInfoResponse toAdContentInfo(AdContent adContent,
            AdvertisementResponse.AdGroupInfoResponse adGroupInfoResponse) {
        return new AdvertisementResponse.AdContentInfoResponse(
                adContent.getId(), adContent.getTrackingUrl(), adContent.getLandingUrl(), adContent.getStatus(), adGroupInfoResponse.targetInfo());
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
}
