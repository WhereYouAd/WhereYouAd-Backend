package com.whereyouad.WhereYouAd.domains.dashboard.presentation;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.domain.service.DashboardService;
import com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs.DashboardControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/dashboard")
public class DashboardController implements DashboardControllerDocs {

    private final DashboardService dashboardService;

    @GetMapping("/{orgId}/metrics")
    public ResponseEntity<DataResponse<DashboardResponse.AggregatedSummaryResponse>> getMetricsSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) String providerType
    )
    {
        DashboardResponse.AggregatedSummaryResponse response = dashboardService.getAggregatedMetrics(userId, orgId, providerType);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @GetMapping("/budgets")
    public ResponseEntity<DataResponse<DashboardResponse.BudgetSummaryResponse>> getBudgetSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(name = "orgId") Long orgId,
            @RequestParam(required = false, name = "providerType") String providerType) {
        DashboardResponse.BudgetSummaryResponse budgetSummaryResponse = dashboardService.getBudgetSummary(userId, orgId,
                providerType);
        return ResponseEntity.ok(DataResponse.from(budgetSummaryResponse));
    }

}
