package com.whereyouad.WhereYouAd.domains.ai.application.dto.response;

import lombok.Builder;

import java.util.List;

public class WeeklyReportResponse {

    @Builder
    public record WeeklyAnalysisResponse(
            String weekSummary,
            KpiOverview kpiOverview,
            List<Highlight> highlights,
            List<PlatformInsight> platformInsights,
            List<String> dataCaveats,
            NextWeekFocus nextWeekFocus
    ) {}

    @Builder
    public record KpiOverview(
            KpiMetric totalSpend,
            KpiMetric totalConversions,
            KpiMetric blendedRoas
    ) {}

    @Builder
    public record KpiMetric(
            double thisWeek,
            double prevWeek,
            double changeRate
    ) {}

    @Builder
    public record Highlight(
            String type,        // POSITIVE | NEGATIVE
            String title,
            String what,
            String why,
            String evidence,
            String confidence,  // HIGH | MEDIUM | LOW
            String action
    ) {}

    @Builder
    public record PlatformInsight(
            String platform,
            String oneLineSummary,
            String keyObservation
    ) {}

    @Builder
    public record NextWeekFocus(
            String focus,
            String reason,
            String expectedOutcome
    ) {}
}
