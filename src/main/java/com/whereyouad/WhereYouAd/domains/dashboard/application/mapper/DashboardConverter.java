package com.whereyouad.WhereYouAd.domains.dashboard.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.BudgetHistory;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DashboardConverter {

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

    public static DashboardResponse.AggregatedSummaryResponse toAggregatedSummary(
            Long clicks, Double clickChangeRate, Long impressions, Double impressionChangeRate,
            Double cvr, Double cvrChangeRate, Double roas, Double roasChangeRate
    )
    {
        return new DashboardResponse.AggregatedSummaryResponse(clicks, clickChangeRate, impressions, impressionChangeRate,
                cvr, cvrChangeRate, roas, roasChangeRate);
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

    public static List<DashboardResponse.BudgetHistoryItem> toBudgetHistoryItems(List<BudgetHistory> histories) {
        return histories.stream().map(bh -> {
            String targetName = bh.getAdCampaign() != null
                    ? bh.getAdCampaign().getName()
                    : bh.getAdGroup().getName();
            return new DashboardResponse.BudgetHistoryItem(
                    bh.getFieldType(),
                    targetName,
                    bh.getPreviousValue(),
                    bh.getNewValue(),
                    bh.getCreatedAt(),
                    bh.getProvider()
            );
        }).toList();
    }

    //Data -> DTO
    public static DashboardResponse.RealTimeGraphResponse toRealTimeGraphResponse(
            String provider,
            List<ClickResponse.RealtimeClickCount> timeSeriesData,
            String mode,
            Boolean hasSuspect,
            DashboardResponse.SuspectDetail suspectDetail)
    {
        return new DashboardResponse.RealTimeGraphResponse(provider ,timeSeriesData, mode, hasSuspect, suspectDetail);
    }
}
