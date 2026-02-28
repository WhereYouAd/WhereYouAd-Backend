package com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response;

public class DashboardResponse {
    public record BudgetSummaryResponse(
            Double usagePercentage,
            Long totalBudget,
            Long totalSpend,
            Long remainingBudget
    ) {}
}
