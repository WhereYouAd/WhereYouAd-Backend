package com.whereyouad.WhereYouAd.domains.dashboard.presentation;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.domain.service.DashboardClickService;
import com.whereyouad.WhereYouAd.domains.dashboard.domain.service.DashboardService;
import com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs.DashboardControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/dashboard")
public class DashboardController implements DashboardControllerDocs {

    private final DashboardService dashboardService;
    private final DashboardClickService dashboardClickService;

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
    @GetMapping("/{orgId}/rankings/roas")
    public ResponseEntity<DataResponse<DashboardResponse.RankingROASList>> getRoasRanking(
            @AuthenticationPrincipal (expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Valid @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
    {
        // 조회 기간 설정이 없으면 오늘부터 최근 1개월 전 데이터 조회
        LocalDate adjustedEnd = (endDate != null) ? endDate : LocalDate.now();
        LocalDate adjustedStart = (startDate != null) ? startDate : adjustedEnd.minusMonths(1);

        DashboardResponse.RankingROASList response = dashboardService.getRoasRanking(
                userId, orgId, adjustedStart, adjustedEnd);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/{orgId}/metric-facts")
    public ResponseEntity<DataResponse<DashboardResponse.PlatformMetricFactSummaryResponse>> getPlatformMetricFacts(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(name = "providerType") String providerType,
            @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        DashboardResponse.PlatformMetricFactSummaryResponse response =
                dashboardService.getPlatformMetricFacts(userId, orgId, providerType, days);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/{orgId}/budget-history")
    public ResponseEntity<DataResponse<DashboardResponse.BudgetHistoryListResponse>> getBudgetHistory(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(DataResponse.from(
                dashboardService.getBudgetHistory(userId, orgId, startDate, endDate)));
    }

    @GetMapping(value = "/{orgId}/clicks/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamRealClicks(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false, defaultValue = "dummy") String mode,
            @RequestParam(required = false) String providerType,
            HttpServletResponse response
    )
    {
        SseEmitter emitter = dashboardClickService.subscribe(userId, orgId, mode, providerType);

        response.setHeader("X-Accel-Buffering", "no");
        return ResponseEntity.ok(emitter);
    }
}
