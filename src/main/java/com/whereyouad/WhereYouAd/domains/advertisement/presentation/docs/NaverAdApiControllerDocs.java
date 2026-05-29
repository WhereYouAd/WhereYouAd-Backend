package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request.AdvertisementRequest;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface NaverAdApiControllerDocs {

    @Operation(summary = "api 통신 test용: 네이버 광고 캠페인 목록 조회", description = "연동된 네이버 광고 계정의 캠페인 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캠페인 목록 반환"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 네이버 연결 정보 없음"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.CampaignResponse>>> getCampaigns(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId
    );

    @Operation(summary = "api 통신 test용: 네이버 광고 그룹 목록 조회", description = "특정 캠페인의 광고 그룹 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "광고 그룹 목록 반환"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 네이버 연결 정보 없음"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.AdGroupResponse>>> getAdGroups(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "네이버 캠페인 ID", required = true)
            @RequestParam("nccCampaignId") String nccCampaignId
    );

    @Operation(summary = "api 통신 test용: 네이버 광고 소재 목록 조회", description = "특정 광고 그룹의 광고 소재 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "광고 소재 목록 반환"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 네이버 연결 정보 없음"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.AdResponse>>> getAds(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "네이버 광고 그룹 ID", required = true)
            @RequestParam("nccAdgroupId") String nccAdgroupId
    );

    @Operation(summary = "api 통신 test용: 네이버 키워드 원문 조회", description = "광고 그룹에 설정된 키워드 목록을 원문으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "키워드 목록 반환"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 네이버 연결 정보 없음"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.KeywordResponse>>> getKeywords(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "네이버 광고 그룹 ID", required = true)
            @RequestParam("nccAdgroupId") String nccAdgroupId
    );
    @Operation(summary = "api 통신 test용: 네이버 AD 리포트 생성 요청", description = "특정 일자의 AD 리포트 생성을 네이버에 요청합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 생성 요청됨(상태 확인 필요)"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> requestAdReport(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "리포트 대상 일자 (예: 20260408)", required = true)
            @RequestParam("statDt") String statDt
    );

    @Operation(summary = "api 통신 test용: 네이버 AD_CONVERSION 리포트 생성 요청", description = "특정 일자의 AD_CONVERSION 리포트 생성을 네이버에 요청합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 생성 요청됨(상태 확인 필요)"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> requestAdConversionReport(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "리포트 대상 일자 (예: 20260408)", required = true)
            @RequestParam("statDt") String statDt
    );

    @Operation(summary = "api 통신 test용: 대용량 보고서 상태 조회", description = "생성 요청한 보고서의 상태를 확인합니다. (BUILT 상태가 되면 다운로드 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 및 다운로드 URL 반환"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> getReportStatus(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "보고서 Job ID", required = true)
            @PathVariable String reportJobId
    );

    @Operation(summary = "api 통신 test용: 대용량 보고서 다운로드", description = "보고서 다운로드 URL을 통해 원문(TSV 등) 데이터를 가져옵니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 데이터 원문 반환"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<NaverDTO.RawReportResponse>> downloadReport(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "다운로드 URL (상태 조회에서 얻은 URL)", required = true)
            @RequestParam("url") String downloadUrl
    );

    @Operation(summary = "api 통신 test용: 일별 통계 직접 조회", description = "/stats API를 이용하여 특정 대상의 일별 기본 지표를 가져옵니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일별 통계 반환"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.StatResponse>>> getDailyStats(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "캠페인 ID 또는 광고 그룹 ID", required = true)
            @RequestParam("id") String id,
            @Parameter(description = "시작 날짜 (예: 2026-04-08)", required = true)
            @RequestParam("since") String since,
            @Parameter(description = "종료 날짜 (예: 2026-04-08)", required = true)
            @RequestParam("until") String until
    );

    @Operation(summary = "api 통신 test용: 네이버 메타데이터 동기화", description = "연동된 네이버 계정의 캠페인/광고그룹/광고소재를 가져와 DB에 upsert합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동기화 완료 - 처리된 캠페인/그룹/소재 수 반환"),
            @ApiResponse(responseCode = "404", description = "커넥션 정보 없음"),
            @ApiResponse(responseCode = "500", description = "동기화 중 오류 발생")
    })
    ResponseEntity<DataResponse<AdvertisementResponse.NaverMetadataSyncResponse>> syncMetadata(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId
    );

    @Operation(summary = "네이버 수동 동기화", description = "조직(orgId)에 연결된 모든 네이버 계정의 메타데이터 + 기간별 통계 + 전환을 동기화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동기화 완료 - 캠페인/그룹/소재/지표 수 및 실패한 연결 ID 반환"),
            @ApiResponse(responseCode = "400", description = "날짜 범위 오류 (startDate > endDate)"),
            @ApiResponse(responseCode = "500", description = "동기화 중 오류 발생")
    })
    ResponseEntity<DataResponse<AdvertisementResponse.NaverManualSyncSummary>> syncManually(
            @Parameter(description = "조직 ID", example = "1", required = true)
            @PathVariable("connectionId") Long orgId,
            @RequestBody AdvertisementRequest.ManualSyncRequest request
    );

    @Operation(summary = "api 통신 test용: 네이버 전체 통계 동기화", description = "일별(DAILY) 기본 지표와 전환 리포트를 동기화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동기화 완료 - 처리된 광고소재 수 반환"),
            @ApiResponse(responseCode = "404", description = "커넥션 정보 없음"),
            @ApiResponse(responseCode = "500", description = "동기화 중 오류 발생")
    })
    ResponseEntity<DataResponse<AdvertisementResponse.NaverStatSyncResponse>> syncStats(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "통계 대상 날짜 (yyyy-MM-dd, 예: 2026-04-08)", required = true)
            @RequestParam("statDate") String statDate
    );

    @Operation(summary = "api 통신 test용: 네이버 전환 리포트만 동기화", description = "전환 데이터만 단독으로 동기화합니다. (기본 Stats 동기화 없이)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동기화 완료 - 처리된 전환 행 수 반환"),
            @ApiResponse(responseCode = "404", description = "커넥션 정보 없음"),
            @ApiResponse(responseCode = "500", description = "동기화 중 오류 발생")
    })
    ResponseEntity<DataResponse<AdvertisementResponse.NaverStatSyncResponse>> syncConversions(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "통계 대상 날짜 (yyyy-MM-dd, 예: 2026-04-08)", required = true)
            @RequestParam("statDate") String statDate
    );

    @Operation(summary = "네이버 캠페인 예산 수정", description = "캠페인의 일일 예산 및 예산 사용 여부를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 캠페인 정보 반환"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<NaverDTO.CampaignResponse>> updateCampaignBudget(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "수정할 캠페인 ID", required = true)
            @PathVariable String campaignId,
            @RequestBody NaverDTO.UpdateCampaignBudgetRequest request
    );

    @Operation(summary = "네이버 광고그룹 예산 수정", description = "광고그룹의 일일 예산, 예산 사용 여부, 입찰가를 수정합니다. bidAmt가 null이면 입찰가는 수정하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 광고그룹 정보 반환"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<NaverDTO.AdGroupResponse>> updateAdGroupBudget(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "수정할 광고그룹 ID", required = true)
            @PathVariable String adgroupId,
            @RequestBody NaverDTO.UpdateAdGroupBudgetRequest request
    );
}
