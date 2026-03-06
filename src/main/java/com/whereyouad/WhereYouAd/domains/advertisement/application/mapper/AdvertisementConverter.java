package com.whereyouad.WhereYouAd.domains.advertisement.application.mapper;


import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;

public class AdvertisementConverter {

    public static AdvertisementResponse.AdContentInfoResponse toAdContentInfo(AdContent adContent, AdvertisementResponse.AdGroupInfoResponse adGroupInfoResponse) {
        return new AdvertisementResponse.AdContentInfoResponse(
                adContent.getId(), adContent.getTrackingUrl(), adContent.getLandingUrl(), adContent.getStatus(), adGroupInfoResponse.targetInfo());
    }

    public static AdvertisementResponse.AdGroupInfoResponse toAdGroupInfo(AdGroup adGroup) {
        return new AdvertisementResponse.AdGroupInfoResponse(
                adGroup.getId(), adGroup.getName(), adGroup.getTargetingInfo(), adGroup.getStatus());
    }
}
