package com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response;

public class DashboardResponse {
    public record BudgetSummaryResponse(
            String providerType,
            Double usagePercentage,
            Long totalBudget,
            Long totalSpend,
            Long remainingBudget
    ) {}

    public record AggregatedSummaryResponse(
            Long clicks, //클릭수
            Double clickChangeRate, //클릭수 변화율

            Long impressions, //노출수
            Double impressionChangeRate, //노출수 변화율

            Double conversion, //전환율
            Double cvrChangeRate,  //전환율 변화율

            Double ROAS, //광고비 대비 매출
            Double ROASChangeRate //광고비 대비 매출 변화율
    ) {}
}
