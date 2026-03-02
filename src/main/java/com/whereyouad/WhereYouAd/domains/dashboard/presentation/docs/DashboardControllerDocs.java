package com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface DashboardControllerDocs {

        @Operation(
                summary = "대시보드 - 전체 지표 집계 API",
                description = "AccessToken 값과 PathVariable 로 orgId 값을 받아 해당 조직 내부에서 진행하는 모든 프로젝트의 광고 지표를 집계합니다\n\n" +
                        "지표값은 clicks(클릭수), impressions(노출수), conversion(전환율), ROAS(광고비 대비 매출) 으로 DB 내부 Mock data 의 가장 최근 데이터 ~ 한달 전 데이터 집계값 입니다.\n\n" +
                        "변화율은 clickChangeRate(클릭수 변화율), impressionChangeRate(노출수 변화율), cvrChangeRate(전환율 변화율), ROASChangeRate(광고비 대비 매출 변화율) 이며, DB 내부 Mock data 의 한달전 ~ 두달전 데이터와 최근 ~ 한달전 의 비교값 입니다.\n\n" +
                        "providerType 을 param 으로 입력하지 않을 시 해당 조직의 모든 지표 합산, providerType 에 KAKAO/NAVER/GOOGLE 입력 시 해당 provider 에 대한 합산 값을 반환합니다."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "성공"),
                @ApiResponse(responseCode = "403_1", description = "조직에 가입되지 않은 회원"),
                @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재X")
        }
        )
        public ResponseEntity<DataResponse<DashboardResponse.AggregatedSummaryResponse>> getMetricsSummary(
                @AuthenticationPrincipal(expression = "userId") Long userId,
                @PathVariable Long orgId,
                @RequestParam(required = false) String providerType
        );

        @Operation(summary = "대시보드 - 예산 소진 현황 조회 API", description = "통합 대시보드는 요청 쿼리 파라미터 없이 호출, 플랫폼 대시보드는 요청 시 쿼리 파라미터에 providerType(GOOGLE, KAKAO, NAVER)과 함께 호출")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "401_1", description = "실패")
        })
        public ResponseEntity<DataResponse<DashboardResponse.BudgetSummaryResponse>> getBudgetSummary(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @RequestParam(name = "orgId") Long orgId,
                        @RequestParam(required = false, name = "providerType") String providerType);
}
