package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;

import java.time.LocalDate;

public interface DashboardService {

    DashboardResponse.BudgetSummaryResponse getBudgetSummary(Long userId, Long orgId, String provider);

    // 지정된 날짜(startDate ~ endDate)동안의 진행 중(ON_GOING) 상태인 광고 개수를 찾는 메서드
    DashboardResponse.OngoingPlatformAdCountResponse getOngoingAdCountByProvider(
            Long userId, Long orgId, LocalDate startDate, LocalDate endDate);
}
