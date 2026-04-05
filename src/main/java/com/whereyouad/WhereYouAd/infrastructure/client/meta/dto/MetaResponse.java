package com.whereyouad.WhereYouAd.infrastructure.client.meta.dto;

public class MetaResponse {

    public record AuthUrlResponse(
            String authUrl
    ) {}

    public record MetaSyncSummary(
            int adCampaignCount,
            int adGroupCount,
            int adContentCount,
            int metricCount
    ) {}
}
