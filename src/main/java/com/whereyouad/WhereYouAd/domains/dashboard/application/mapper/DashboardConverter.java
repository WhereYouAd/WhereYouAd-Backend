package com.whereyouad.WhereYouAd.domains.dashboard.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

import java.math.BigDecimal;

public class DashboardConverter {

    public static DashboardResponse.BudgetSummaryResponse toBudgetSummary(
            String providerType, Double usagePercentage, Long totalBudget, Long totalSpend, Long remainingBudget) {
        return new DashboardResponse.BudgetSummaryResponse(providerType, usagePercentage, totalBudget, totalSpend,
                remainingBudget);
    }

    public static DashboardResponse.RankingROAS toRankingROAS(
            Integer rank,
            Provider provider,
            Double roas,
            Integer diffRate,
            BigDecimal totalRevenue,
            BigDecimal totalSpend) {
        return new DashboardResponse.RankingROAS(
                rank,
                provider,
                Math.round(roas * 100.0) / 100.0, // 소수점 2자리 반올림
                diffRate,
                totalRevenue != null ? totalRevenue.longValue() : 0L,
                totalSpend != null ? totalSpend.longValue() : 0L);
    }
}
