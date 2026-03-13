package com.whereyouad.WhereYouAd.domains.dashboard.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @Operation(
            summary = "ROAS 성과 순위 조회",
            description = """
            특정 조직(org)에 속한 모든 프로젝트의 광고 플랫폼(Provider)별 ROAS 성과 순위를 내림차순으로 반환합니다.

            **[조회 기간(startDate, endDate) 설정]**
            - 특정 기간을 지정하여 데이터를 조회할 수 있습니다. (예: 2026-03-01)
            - 파라미터를 입력하지 않으면, 기본값으로 **최근 1개월(오늘 기준 한 달 전 ~ 오늘)**의 데이터를 조회합니다.

            **[변화율(diffRate) 계산 기준]**
            - 지정된 조회 기간과 **동일한 일수만큼의 직전 기간** 데이터를 바탕으로 ROAS 변화율(%)을 계산합니다.
            - (예: 7일 치 데이터를 조회하면 직전 7일 치와 비교, 1개월 치 조회 시 직전 1개월 치와 비교)
            - 이전 동일 기간에 비교할 기초 데이터(광고비 등)가 존재하지 않는 경우 `null`을 반환합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (결과 없으면 빈 배열)"),
            @ApiResponse(responseCode = "400", description = "시작일이 종료일보다 늦은 경우"),
            @ApiResponse(responseCode = "403", description = "해당 조직에 대한 접근 권한이 없는 경우"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 조직")
    })
    ResponseEntity<DataResponse<DashboardResponse.RankingROASList>> getRoasRanking(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "조직 ID", required = true, example = "1") Long orgId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)") LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)") LocalDate endDate
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


    @Operation(
            summary = "대시보드 - 실시간 클릭수 스트림 출력 API",
            description = "해당 조직의 실시간 클릭수를 스트림으로 보내주는 API 입니다. providerType 값을 입력하지 않으면 조직 내 모든 광고에 대해, 입력시 해당 플랫폼에 대해 클릭수를 스트림으로 반환합니다.\n\n" +
                    "SSE(Server-Sent-Events) 방식을 사용하므로 한 번 연결되면 1초마다 데이터가 지속적으로 푸시됩니다.\n\n" +
                    "### 🚨 프론트엔드 연동 시 주의사항\n" +
                    "본 API는 JWT 인증(`Authorization` 헤더)이 필수입니다. " +
                    "하지만 브라우저 기본 내장 객체인 `new EventSource()`는 구조상 커스텀 헤더 전송을 지원하지 않아 401 Unauthorized 에러가 발생합니다.\n\n" +
                    "따라서 프론트엔드에서는 **`@microsoft/fetch-event-source`** 와 같은 외부 라이브러리를 사용하여 " +
                    "헤더에 `Authorization: Bearer {AccessToken}`을 반드시 담아 호출해 주시기 바랍니다.\n\n" +
                    "---\n" +
                    "* **수신 이벤트 명(Event Name):** `org-click-update`\n" +
                    "* **데이터 규격:** 기존 API와 동일하게 파싱 (`response.data.currentClickCount`, `response.data.providerType`)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "ORG_404_1 : 해당 id 의 조직이 존재하지 않습니다. \n\n" +
                    "ORG_404_2 : 해당 멤버가 조직에 존재하지 않습니다.")
    })
    public ResponseEntity<SseEmitter> streamRealClicks(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) Provider provider
    );
}