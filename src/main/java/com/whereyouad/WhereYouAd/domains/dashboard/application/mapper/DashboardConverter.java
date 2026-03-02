package com.whereyouad.WhereYouAd.domains.dashboard.application.mapper;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

import java.time.LocalDate;
import java.util.List;

public class DashboardConverter {

    public static DashboardResponse.BudgetSummaryResponse toBudgetSummary(
            String providerType, Double usagePercentage, Long totalBudget, Long totalSpend, Long remainingBudget) {
        return new DashboardResponse.BudgetSummaryResponse(providerType, usagePercentage, totalBudget, totalSpend,
                remainingBudget);
    }

    public static DashboardResponse.OngoingPlatformAdCountResponse toOngoingPlatformAdCountResponse(
            List<DashboardResponse.OngoingPlatformAdCount> providerCount,
            LocalDate startDate,
            LocalDate endDate)
    {
        long totalCount = providerCount.stream()
                .mapToLong(DashboardResponse.OngoingPlatformAdCount::count)
                .sum();
        return new DashboardResponse.OngoingPlatformAdCountResponse(startDate, endDate, totalCount, providerCount);
    }
}
