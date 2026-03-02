package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse.BudgetSummaryResponse getBudgetSummary(Long userId, Long orgId, String provider);

    DashboardResponse.AggregatedSummaryResponse getAggregatedMetrics(Long userId, Long orgId, String providerType);

}
