package com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response;


import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;

import java.util.List;

public class AdvertisementResponse {
    public record AdContentInfoResponse (
            Long id,
            String name,
            Provider provider,
            String trackingUrl,
            String landingUrl,
            String description,
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

    // 네이버 광고 메타데이터 동기화 결과
    public record NaverMetadataSyncResponse(
            Long connectionId,
            int syncedCampaignCount,
            int syncedAdGroupCount,
            int syncedAdContentCount
    ) {}

    // 네이버 광고 통계 동기화 결과
    public record NaverStatSyncResponse(
            Long connectionId,
            String statDate,
            int processedAdContentCount
    ) {}

    // 네이버 수동 동기화 결과
    public record NaverManualSyncSummary(
            int adCampaignCount,
            int adGroupCount,
            int adContentCount,
            int metricCount,
            List<Long> failedConnectionIds
    ) {}
}
