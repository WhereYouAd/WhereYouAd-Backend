package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;

@Tag(name = "Advertisement", description = "광고 성과 관련 API")
public interface AdvertisementControllerDocs {

    @Operation(summary = "ROAS 성과 순위 조회", description = """
        특정 조직(org)에 속한 모든 프로젝트의 광고 플랫폼(Provider)별 ROAS 성과 순위를 내림차순으로 반환합니다.

        **[조회 기간(startDate, endDate) 설정]**
        - 특정 기간을 지정하여 데이터를 조회할 수 있습니다. (예: 2026-03-01)
        - 파라미터를 입력하지 않으면, 기본값으로 **최근 1개월(오늘 기준 한 달 전 ~ 오늘)**의 데이터를 조회합니다.

        **[변화율(diffRate) 계산 기준]**
        - 지정된 조회 기간과 **동일한 일수만큼의 직전 기간** 데이터를 바탕으로 ROAS 변화율(%)을 계산합니다.
        - (예: 7일 치 데이터를 조회하면 직전 7일 치와 비교, 1개월 치 조회 시 직전 1개월 치와 비교)
        - 이전 동일 기간에 비교할 기초 데이터(광고비 등)가 존재하지 않는 경우 `null`을 반환합니다.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (결과 없으면 빈 배열)"),
            @ApiResponse(responseCode = "400", description = "시작일이 종료일보다 늦은 경우"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 조직")
    })
    ResponseEntity<DataResponse<AdvertisementResponse.RankingROASList>> getRoasRanking(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "조직 ID", required = true, example = "1") Long orgId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)") LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)") LocalDate endDate);
}
