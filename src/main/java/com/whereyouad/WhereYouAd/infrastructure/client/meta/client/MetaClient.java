package com.whereyouad.WhereYouAd.infrastructure.client.meta.client;

import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "metaClient",
        url = "https://graph.facebook.com/v25.0"
)
public interface MetaClient {

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
            @RequestParam(value = "after", required = false) String afterCursor);

}
