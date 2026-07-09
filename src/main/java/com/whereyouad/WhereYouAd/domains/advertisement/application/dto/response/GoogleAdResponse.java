package com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;

public class GoogleAdResponse {
    public record GoogleAdCreateResponse(
            String message
    ){}

    public record BudgetUpdateResponse(
            Long adCampaignId,
            String externalCampaignId,
            Long amount,
            BudgetType budgetType
    ) {}
}
