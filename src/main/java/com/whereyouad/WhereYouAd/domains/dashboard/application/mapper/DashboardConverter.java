package com.whereyouad.WhereYouAd.domains.dashboard.application.mapper;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

public class DashboardConverter {

    public static DashboardResponse.BudgetSummaryResponse toBudgetSummary(
            String providerType, Double usagePercentage, Long totalBudget, Long totalSpend, Long remainingBudget) {
        return new DashboardResponse.BudgetSummaryResponse(providerType, usagePercentage, totalBudget, totalSpend,
                remainingBudget);
    }
}
