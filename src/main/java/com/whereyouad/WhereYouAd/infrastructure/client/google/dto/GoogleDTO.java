package com.whereyouad.WhereYouAd.infrastructure.client.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

public class GoogleDTO {

    // 1. 캠페인 조회용 DTO
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdCampaignResponse {
        private List<AdCampaignResult> results;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdCampaignResult {
        private AdCampaignNode campaign;
        private AdCampaignBudgetNode campaignBudget;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdCampaignNode {
        private String id;
        private String name;
        private String status;
        private String startDate;
        private String endDate;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdCampaignBudgetNode {
        private Long amountMicros;
    }

    // 2. 광고 그룹 조회용 DTO
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdGroupResponse {
        private List<AdGroupResult> results;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdGroupResult {
        private AdGroupNode adGroup;
        private AdGroupCampaignNode campaign;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdGroupNode {
        private String id;
        private String name;
        private String status;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdGroupCampaignNode {
        private String id;
    }

    // 3. 개별 광고 조회용 DTO
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdContentResponse {
        private List<AdContentResult> results;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdContentResult {
        private AdGroupAdNode adGroupAd;
        private AdContentGroupNode adGroup;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdGroupAdNode {
        private String status;
        private AdNode ad;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdNode {
        private String id;
        private String name;
        private String type;
        private String trackingUrlTemplate;

        private List<String> finalUrls;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdContentGroupNode {
        private String id;
    }

    // 4. 통계(MetricFact) 조회용 DTO
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricFactResponse {
        private List<MetricFactResult> results;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricFactResult {
        private MetricsNode metrics;
        private SegmentsNode segments;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricsNode {
        private Long impressions;
        private Long clicks;
        private Double conversions;
        private Long costMicros;
        private Double conversionsValue;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentsNode {
        private String date;
    }
}