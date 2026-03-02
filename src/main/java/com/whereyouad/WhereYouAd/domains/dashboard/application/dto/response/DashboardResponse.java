package com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.time.LocalDate;
import java.util.List;

public class DashboardResponse {
    public record BudgetSummaryResponse(
            String providerType,
            Double usagePercentage,
            Long totalBudget,
            Long totalSpend,
            Long remainingBudget
    ) {}

    // 현재 진행 중인 모든 플랫폼의 광고 개수 반환 응답
    public record OngoingPlatformAdCountResponse(
            LocalDate startDate,
            LocalDate endDate,
            Long totalCount,
            List<OngoingPlatformAdCount> providerCount
    ) {}
    // 각 플랫폼에 해당하는 광고 개수
    public record OngoingPlatformAdCount(
            Provider provider,
            Long count
    ) {}
}
