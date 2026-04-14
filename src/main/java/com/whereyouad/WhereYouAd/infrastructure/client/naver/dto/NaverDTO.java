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
}
