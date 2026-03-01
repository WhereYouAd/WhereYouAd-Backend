package com.whereyouad.WhereYouAd.domains.dashboard.presentation;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.domain.service.DashboardService;
import com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs.DashboardControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/dashboard")
public class DashboardController implements DashboardControllerDocs {

    private final DashboardService dashboardService;

    @GetMapping("/budgets")
    public ResponseEntity<DataResponse<DashboardResponse.BudgetSummaryResponse>> getBudgetSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(required = false, name = "providerType") String providerType) {
        DashboardResponse.BudgetSummaryResponse budgetSummaryResponse = dashboardService.getBudgetSummary(userId,
                providerType);
        return ResponseEntity.ok(DataResponse.from(budgetSummaryResponse));
    }

}
