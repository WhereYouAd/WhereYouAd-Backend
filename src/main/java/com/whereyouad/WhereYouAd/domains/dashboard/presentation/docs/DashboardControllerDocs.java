package com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface DashboardControllerDocs {

        @Operation(summary = "대시보드 - 예산 소진 현황 조회 API", description = "통합 대시보드는 요청 쿼리 파라미터 없이 호출, 플랫폼 대시보드는 요청 시 쿼리 파라미터에 providerType(GOOGLE, KAKAO, NAVER)과 함께 호출")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "401_1", description = "실패")
        })
        public ResponseEntity<DataResponse<DashboardResponse.BudgetSummaryResponse>> getBudgetSummary(
                        @RequestParam(required = false, name = "providerType") String providerType);
}
