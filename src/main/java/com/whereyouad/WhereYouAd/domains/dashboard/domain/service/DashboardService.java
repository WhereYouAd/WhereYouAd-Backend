package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

import java.time.LocalDate;

public interface DashboardService {

    DashboardResponse.BudgetSummaryResponse getBudgetSummary(Long userId, Long orgId, String provider);

    DashboardResponse.AggregatedSummaryResponse getAggregatedMetrics(Long userId, Long orgId, String providerType);


    // 특정 조직의 전체 광고에 대한 기간별 ROAS 성과 순위 조회
    DashboardResponse.RankingROASList getRoasRanking(Long userId, Long orgId, LocalDate startDate, LocalDate endDate);
}
