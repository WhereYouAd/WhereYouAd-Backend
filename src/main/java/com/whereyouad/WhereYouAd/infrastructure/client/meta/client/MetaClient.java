package com.whereyouad.WhereYouAd.infrastructure.client.meta.client;

import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "metaClient",
        url = "https://graph.facebook.com/v25.0"
)
public interface MetaClient {

    // 인가 코드 → short-lived Access Token
    @GetMapping("/oauth/access_token")
    MetaDTO.TokenResponse exchangeCodeForToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code") String code);

    // short-lived → long-lived Access Token
    @GetMapping("/oauth/access_token")
    MetaDTO.TokenResponse exchangeForLongLivedToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("fb_exchange_token") String fbExchangeToken);

    // 내 광고계정 목록 조회
    @GetMapping("/me/adaccounts")
    MetaDTO.AdAccountListResponse getAdAccounts(
            @RequestParam("access_token") String accessToken,
            @RequestParam("fields") String fields);

    // 캠페인(AdCampaign) 목록 조회
    @GetMapping("/{adAccountId}/campaigns")
    MetaDTO.CampaignListResponse getCampaigns(
            @RequestParam("access_token") String accessToken,
            @PathVariable("adAccountId") String adAccountId,
            @RequestParam("fields") String fields,
            @RequestParam(value = "after", required = false) String afterCursor);

    // 광고세트(AdSet -> AdGroup) 목록 조회
    @GetMapping("/{adAccountId}/adsets")
    MetaDTO.AdSetListResponse getAdSets(
            @RequestParam("access_token") String accessToken,
            @PathVariable("adAccountId") String adAccountId,
            @RequestParam("fields") String fields,
            @RequestParam(value = "after", required = false) String afterCursor);

    // 광고(Ad -> AdContent) 목록 조회
    @GetMapping("/{adAccountId}/ads")
    MetaDTO.AdListResponse getAds(
            @RequestParam("access_token") String accessToken,
            @PathVariable("adAccountId") String adAccountId,
            @RequestParam("fields") String fields,
            @RequestParam(value = "after", required = false) String afterCursor);

    // 광고계정 인사이트(성과 보고서 -> MetricFact) 조회
    @GetMapping("/{adAccountId}/insights")
    MetaDTO.InsightListResponse getInsights(
            @RequestParam("access_token") String accessToken,
            @PathVariable("adAccountId") String adAccountId,
            @RequestParam("fields") String fields,
            @RequestParam("level") String level,
            @RequestParam("time_range") String timeRange,
            @RequestParam("time_increment") String timeIncrement,
            // 어트리뷰션 윈도우 명시 (전환/매출 수치 재현성 확보)
            @RequestParam(value = "action_attribution_windows", required = false) String actionAttributionWindows,
            @RequestParam(value = "after", required = false) String afterCursor);

    @PostMapping("/{nodeId}")
    MetaDTO.UpdateResponse updateBudget(
            @PathVariable("nodeId") String nodeId,
            @RequestParam("access_token") String accessToken,
            @RequestParam(value = "daily_budget", required = false) Long dailyBudget,
            @RequestParam(value = "lifetime_budget", required = false) Long lifetimeBudget
    );
}
