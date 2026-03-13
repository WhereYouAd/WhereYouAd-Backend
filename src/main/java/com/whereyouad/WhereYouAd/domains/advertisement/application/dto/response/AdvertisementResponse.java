package com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response;


import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;

import java.util.List;

public class AdvertisementResponse {
    public record AdContentInfoResponse (
            Long id,
            String trackingUrl,
            String landingUrl,
            Status status,
            String targetInfo
    ) {}

    public record AdContentInfosResponse (
            List<AdContentInfoResponse> adContentInfoResponses
    ) {}

    public record AdGroupInfoResponse (
            Long id,
            String name,
            String targetInfo,
            Status status
    ) {}

    public record AdCampaignListResponse(
            List<AdCampaignSimpleResponse> adCampaigns
    ) {}

    public record AdCampaignSimpleResponse(
            Long adCampaignId,
            String name,
            String description
    ) {}
}
