package com.whereyouad.WhereYouAd.domains.ai.presentation.docs;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.request.AIRequest;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AIControllerDocs {

    @Operation(summary = "AI 광고 성과 분석 요청 API", description = """
            특정 조직(orgId)의 기간(startDate ~ endDate) 동안의 광고 성과 데이터를 AI가 분석하도록 요청합니다.

            **[처리 흐름]**
            1. 요청을 수신하면 즉시 `ai_insight_report` 테이블에 `PENDING` 상태로 저장합니다.
            2. AI 분석은 백그라운드(@Async)에서 비동기로 실행됩니다.
            3. 응답으로 `reportId`를 반환합니다. (202 Accepted)
            4. 클라이언트는 반환된 `reportId`로 GET API를 호출해 결과를 확인합니다.

            **[AI 분석 데이터]**
            - 캠페인별 예산(budget) vs 실제 소진액(spend) 및 소진율(%)
            - 기간 내 일별 광고 성과 원본 데이터 (노출, 클릭, 전환, 광고비, 매출)

            **[응답 구조]**
            - `data`: 생성된 분석 리포트의 `reportId` (Long)
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "분석 요청 접수 성공 — reportId 반환"),
            @ApiResponse(responseCode = "400_1", description = "시작 날짜가 종료 날짜보다 늦음 (INVALID_DATE_RANGE)"),
            @ApiResponse(responseCode = "403_1", description = "해당 조직에 가입되지 않은 사용자 (AI_ACCESS_FORBIDDEN)"),
            @ApiResponse(responseCode = "404_1", description = "해당 조직이 존재하지 않음 (ORG_NOT_FOUND)"),
            @ApiResponse(responseCode = "404_2", description = "해당 기간의 광고 데이터 없음 (NO_METRIC_DATA)")
    })
    ResponseEntity<DataResponse<Long>> requestAnalysis(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "조직 ID", required = true, example = "1") @PathVariable Long orgId,
            @RequestBody @Valid AIRequest.PeriodRequest request);

    @Operation(summary = "AI 광고 성과 분석 결과 조회 API", description = """
            `requestAnalysis` API로 요청한 분석의 결과를 `reportId`로 조회합니다.

            **[status 값]**
            - `PENDING` : AI 분석 진행 중 — `result`는 `null`
            - `SUCCESS` : AI 분석 완료 — `result`에 분석 결과 포함
            - `FAILED`  : AI 분석 실패 (OpenAI 오류 등) — `result`는 `null`

            **[result 필드 구조 (SUCCESS 시)]**
            - `strategySuggestion` : 예산 소진율을 고려한 광고 운영 전략 제안
            - `performanceSummary` : 전체 광고 성과 및 예산 소진 현황 요약
            - `analysisReason`     : 성과 원인 분석 (트렌드, 피크일, 부진일, 예산 영향)
            - `performancePoint`   : 성과가 좋은 날짜/구간/지표 목록
            - `cautionPoint`       : 개선이 필요하거나 예산 초과 위험 구간 목록

            AI 응답에 보통 10~30초 소요
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (PENDING / SUCCESS / FAILED)"),
            @ApiResponse(responseCode = "403_1", description = "해당 조직에 가입되지 않은 사용자 (AI_ACCESS_FORBIDDEN)"),
            @ApiResponse(responseCode = "404_1", description = "해당 조직이 존재하지 않음 (ORG_NOT_FOUND)"),
            @ApiResponse(responseCode = "404_2", description = "해당 reportId의 분석 리포트 없음 (REPORT_NOT_FOUND)")
    })
    ResponseEntity<DataResponse<AIResponse.ReportStatusResponse>> getReport(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "조직 ID", required = true, example = "1") @PathVariable Long orgId,
            @Parameter(description = "분석 리포트 ID", required = true, example = "42") @PathVariable Long reportId);
}
