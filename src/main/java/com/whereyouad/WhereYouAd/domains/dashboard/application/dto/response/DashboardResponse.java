package com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response;

public class DashboardResponse {
    public record BudgetSummaryResponse(
            String providerType,
            Double usagePercentage,
            Long totalBudget,
            Long totalSpend,
            Long remainingBudget
    ) {}
}
