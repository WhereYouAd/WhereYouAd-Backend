package com.whereyouad.WhereYouAd.domains.timeline.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetFieldType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimelineResponse {

    public record CreateResponseDTO(
            Long timelineId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            List<MetricType> metrics,
            LocalDate comparisonStartDate,
            LocalDate comparisonEndDate,
            PerformanceStatus performanceStatus,
            LocalDateTime createdAt
    ) {}

    public record TimelineSummaryDTO(
            Long timelineId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            PerformanceStatus performanceStatus
    ) {}

    public record TimelineDetailDTO(
            Long timelineId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            PerformanceStatus performanceStatus,
            ComparisonPeriodType comparisonPeriodType,
            List<MetricType> metrics,
            String summary,
            List<DailyMetricDTO> dailyTrend,
            List<PlatformContributionDTO> platformContributions,
            List<BudgetHistoryItem> budgetHistories
    ) {}

    public record BudgetHistoryItem(
            BudgetFieldType fieldType,
            String targetName,
            Long previousValue,
            Long newValue,
            LocalDateTime changedAt,
            Provider provider
    ) {}

    public record DailyMetricDTO(
            LocalDate date,
            Long clicks,
            Long conversions,
            Long impressions,
            BigDecimal roas
    ) {}

    public record PlatformContributionDTO(
            String platform,
            double contributionRate
    ) {}
}
