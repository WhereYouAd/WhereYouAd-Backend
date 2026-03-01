package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse.BudgetSummaryResponse getBudgetSummary(Long userId, Long orgId, String provider);
}
