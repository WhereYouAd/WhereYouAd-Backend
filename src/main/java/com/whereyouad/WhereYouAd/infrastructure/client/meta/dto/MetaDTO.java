package com.whereyouad.WhereYouAd.infrastructure.client.meta.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MetaDTO {
    // Meta 응답 원문

    // ========================
    // 1. OAuth 토큰 응답
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn
    ) {}

    // ========================
    // 2. 광고계정 (Ad Account -> PlatformAccount)
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdAccount(
            String id,
            String name,
            @JsonProperty("account_id") String accountId,
            @JsonProperty("account_status") Integer accountStatus,
            @JsonProperty("currency") String currency,
            @JsonProperty("timezone_name") String timezoneName
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdAccountListResponse(
            List<AdAccount> data,
            Paging paging
    ) {}

    // ========================
    // 3. 캠페인 (Campaign → AdCampaign)
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Campaign(
            String id,
            String name,
            String status,
            String objective,
            @JsonProperty("daily_budget") String dailyBudget,
            @JsonProperty("lifetime_budget") String lifetimeBudget,
            @JsonProperty("start_time") String startTime,
            @JsonProperty("stop_time") String stopTime
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CampaignListResponse(
            List<Campaign> data,
            Paging paging
    ) {}

    // ========================
    // 4. 광고세트 (Ad Set → AdGroup)
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdSet(
            String id,
            @JsonProperty("campaign_id") String campaignId,
            String name,
            String status,
            @JsonProperty("daily_budget") String dailyBudget,
            @JsonProperty("lifetime_budget") String lifetimeBudget,
            @JsonProperty("targeting") Targeting targeting,
            @JsonProperty("billing_event") String billingEvent,
            @JsonProperty("optimization_goal") String optimizationGoal
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Targeting(
            @JsonProperty("geo_locations") GeoLocations geoLocations,
            @JsonProperty("age_min") Integer ageMin,
            @JsonProperty("age_max") Integer ageMax
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoLocations(
            List<String> countries
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdSetListResponse(
            List<AdSet> data,
            Paging paging
    ) {}

    // ========================
    // 5. 광고 (Ad → AdContent)
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ad(
            String id,
            @JsonProperty("adset_id") String adSetId,
            String name,
            String status,
            @JsonProperty("creative") AdCreativeRef creative
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdCreativeRef(
            String id,
            String body,
            @JsonProperty("object_type") String objectType
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdListResponse(
            List<Ad> data,
            Paging paging
    ) {}

    // ========================
    // 6. 인사이트 (Insight → MetricFact)
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Insight(
            @JsonProperty("ad_id") String adId,
            @JsonProperty("campaign_id") String campaignId,
            @JsonProperty("adset_id") String adSetId,
            String impressions,
            @JsonProperty("inline_link_clicks") String inlineLinkClicks, // 실제 링크 클릭수를 집계
            String spend,
            @JsonProperty("date_start") String dateStart,
            @JsonProperty("date_stop") String dateStop,
            List<Action> actions,
            // 전환 가치(매출) 집계용 배열
            @JsonProperty("action_values") List<Action> actionValues
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Action(
            @JsonProperty("action_type") String actionType,
            String value
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InsightListResponse(
            List<Insight> data,
            Paging paging
    ) {}

    // ========================
    // 공통: 페이지네이션
    // ========================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Paging(
            Cursors cursors,
            String next,
            String previous
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cursors(
            String before,
            String after
    ) {}

    // 추가 : 예산 변경
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpdateResponse(
            Boolean success,
            String id
    ) {}
}
