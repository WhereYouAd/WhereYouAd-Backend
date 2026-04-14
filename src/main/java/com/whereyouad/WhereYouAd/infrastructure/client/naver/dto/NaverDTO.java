package com.whereyouad.WhereYouAd.infrastructure.client.naver.dto;

public class NaverDTO {

    // 캠페인 응답 원문
    public record CampaignResponse(
            String nccCampaignId,
            Long customerId,
            String campaignTp,
            String name,
            String status,
            String statusReason,
            Boolean useDailyBudget,
            Long dailyBudget,
            Boolean usePeriod,
            String periodStartDt,
            String periodEndDt,
            String deliveryMethod,
            String trackingMode,
            String trackingUrl,
            String trackingUrlCustomParams,
            Boolean userLock,
            Integer numberInUse,

            // 공유 예산 관련
            String sharedBudgetId,
            String sharedBudgetName,
            Long sharedDailyBudget,
            String sharedBudgetDeliveryMethod,
            Long sharedBudgetExpectCost,
            Boolean sharedBudgetLock,

            // 등록/수정 시간
            String regTm,
            String editTm
    ) {}

    // 광고 그룹 응답
    public record AdGroupResponse(
            String nccAdgroupId,
            String nccCampaignId,
            Long customerId,
            String name,
            String status,
            String statusReason,
            Long bidAmt,
            Boolean useDailyBudget,
            Long dailyBudget,
            Boolean userLock,
            String regTm,
            String editTm
    ) {}

    // 광고 소재 응답
    public record AdResponse(
            String nccAdId,
            String nccAdgroupId,
            Long customerId,
            String type,
            String status,
            String statusReason,
            String inspectStatus,
            Boolean userLock,
            AdDetail ad,
            String regTm,
            String editTm
    ) {
        public record AdDetail(
                String headline,
                String description,
                String pcUrl,
                String mobileUrl,
                String displayUrl
        ) {}
    }

    // 키워드 응답 (Ad_Group에서 target_info값)
    public record KeywordResponse(
            String nccKeywordId,
            String nccAdgroupId,
            Long customerId,
            String keyword,
            String status,
            Long bidAmt,
            Boolean useGroupBidAmt,
            String regTm,
            String editTm
    ) {}
}
