package com.whereyouad.WhereYouAd.domains.dashboard.presentation;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.domain.service.DashboardService;
import com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs.DashboardControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/dashboard")
public class DashboardController implements DashboardControllerDocs {

    private final DashboardService dashboardService;

    @GetMapping("/budgets")
    public ResponseEntity<DataResponse<DashboardResponse.BudgetSummaryResponse>> getBudgetSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(name = "orgId") Long orgId,
            @RequestParam(required = false, name = "providerType") String providerType) {
        DashboardResponse.BudgetSummaryResponse budgetSummaryResponse = dashboardService.getBudgetSummary(userId, orgId,
                providerType);
        return ResponseEntity.ok(DataResponse.from(budgetSummaryResponse));
    }

    @GetMapping("/{orgId}/ad-count")
    public ResponseEntity<DataResponse<DashboardResponse.OngoingPlatformAdCountResponse>> getOngoingAdCount(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
    {
        // 조회 기간 설정이 없으면 오늘부터 최근 1개월 데이터 조회
        LocalDate adjustedEnd = (endDate != null) ? endDate : LocalDate.now();
        LocalDate adjustedStart = (startDate != null) ? startDate : adjustedEnd.minusMonths(1);

        DashboardResponse.OngoingPlatformAdCountResponse response = dashboardService.getOngoingAdCountByProvider(
                userId, orgId, adjustedStart, adjustedEnd);

        return ResponseEntity.ok(DataResponse.from(response));
    }
}
