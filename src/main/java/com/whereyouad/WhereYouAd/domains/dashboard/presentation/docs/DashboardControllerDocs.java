package com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

public interface DashboardControllerDocs {

    @Operation(
            summary = "대시보드 - 예산 소진 현황 조회 API",
            description = "통합 대시보드는 요청 쿼리 파라미터 없이 호출, 플랫폼 대시보드는 요청 시 쿼리 파라미터에 providerType(GOOGLE, KAKAO, NAVER)과 함께 호출"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401_1", description = "실패")
    })
    ResponseEntity<DataResponse<DashboardResponse.BudgetSummaryResponse>> getBudgetSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(name = "orgId") Long orgId,
            @RequestParam(required = false, name = "providerType") String providerType
    );

    @Operation(
            summary = "대시보드 - 진행 중인 광고 수 provider별 조회 API",
            description = "조직 내 현재 진행 중인(status=ON_GOING, 기간 포함) 광고를 플랫폼별로 집계해 반환. startDate/endDate 미제공 시 오늘 기준으로 조회."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "조직 없음 또는 멤버 아님")
    })
    ResponseEntity<DataResponse<DashboardResponse.OngoingPlatformAdCountResponse>> getOngoingAdCount(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );
}