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
import org.springframework.web.bind.annotation.RequestParam;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetails;

public interface AIControllerDocs {

    @Operation(summary = "AI 광고 성과 분석 요청 API", description = """
            특정 프로젝트(projectId) 기반으로 조직(orgId)의 기간(startDate ~ endDate) 동안의 광고 성과 데이터를 AI가 분석하도록 요청합니다.

            **[처리 흐름]**
            1. 요청을 수신하면 즉시 `ai_insight_report` 테이블에 `PENDING` 상태로 저장합니다.
            2. AI 분석은 백그라운드(@Async)에서 비동기로 실행됩니다.
            3. 응답으로 `accessToken`을 반환합니다. (202 Accepted)
            4. 클라이언트는 반환된 `accessToken`으로 GET API를 호출해(공유 링크 포함) 결과를 확인합니다.

            **[AI 분석 데이터]**
            - 캠페인별 예산(budget) vs 실제 소진액(spend) 및 소진율(%)
            - 기간 내 일별 광고 성과 원본 데이터 (노출, 클릭, 전환, 광고비, 매출)

            **[응답 구조]**
            - `data`: 생성된 분석 리포트의 `accessToken` (String)

            공유 가능한 형태의 토큰이 발급됩니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "분석 요청 접수 성공 — accessToken 반환"),
            @ApiResponse(responseCode = "400_1", description = "시작 날짜가 종료 날짜보다 늦음 (INVALID_DATE_RANGE)"),
            @ApiResponse(responseCode = "403_1", description = "해당 조직에 가입되지 않은 사용자 (AI_ACCESS_FORBIDDEN)"),
            @ApiResponse(responseCode = "404_1", description = "해당 조직이 존재하지 않음 (ORG_NOT_FOUND)"),
            @ApiResponse(responseCode = "404_2", description = "해당 기간의 광고 데이터 없음 (NO_METRIC_DATA)")
    })
    ResponseEntity<DataResponse<String>> requestAnalysis(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "프로젝트 ID", required = true, example = "1") @PathVariable Long projectId,
            @RequestBody @Valid AIRequest.PeriodRequest request
    );

    @Operation(summary = "AI 광고 성과 분석 공유 링크 결과 조회 API", description = """
            `requestAnalysis` API로 발급받은 `accessToken`으로 분석 결과를 조회합니다. (권한 불필요)

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
            @ApiResponse(responseCode = "403_1", description = "비공개 상태에서 권한 없는 사용자의 접근 (AI_ACCESS_FORBIDDEN)"),
            @ApiResponse(responseCode = "404", description = "해당 accessToken의 분석 리포트 없음 (REPORT_NOT_FOUND)")
    })
    ResponseEntity<DataResponse<AIResponse.ReportStatusResponse>> getReportByAccessToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "리포트 접근 토큰", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String accessToken
    );

    @Operation(summary = "AI 광고 성과 분석 리포트 공유 상태 변경", description = "발급된 리포트의 공유 여부 변경(조직 멤버만 변경 가능)")
    ResponseEntity<DataResponse<String>> updateShareStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "리포트 접근 토큰", required = true) @PathVariable String accessToken,
            @Parameter(description = "공유 ON/OFF 상태값", required = true) @RequestParam boolean isShared
    );
}