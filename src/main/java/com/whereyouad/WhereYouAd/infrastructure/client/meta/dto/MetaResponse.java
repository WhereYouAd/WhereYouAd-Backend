package com.whereyouad.WhereYouAd.infrastructure.client.meta.dto;

import java.util.List;

public class MetaResponse {

    public record AuthUrlResponse(
            String authUrl
    ) {}

    public record MetaSyncSummary(
            int adCampaignCount,
            int adGroupCount,
            int adContentCount,
            int metricCount,
            List<String> failedAccountIds
    ) {}

    public record BudgetUpdateResponse(
            Long targetId,
            String externalId,
            Long updatedBudget,
            String budgetType
    ) {}
}
