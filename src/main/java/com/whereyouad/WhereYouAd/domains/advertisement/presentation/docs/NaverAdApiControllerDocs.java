package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "api 통신 test용: HOURLY (시간대별) 통계 리포트 직접 조회", description = "/stats API를 이용하여 특정 대상의 시간대별 데이터를 가져옵니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시간대별 통계 반환"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.StatResponse>>> getHourlyStats(
            @Parameter(description = "네이버 커넥션 ID", example = "1", required = true)
            @PathVariable Long connectionId,
            @Parameter(description = "캠페인 ID 또는 광고 그룹 ID", required = true)
            @RequestParam("id") String id,
            @Parameter(description = "시작 날짜 (예: 2026-04-08)", required = true)
            @RequestParam("since") String since,
            @Parameter(description = "종료 날짜 (예: 2026-04-08)", required = true)
            @RequestParam("until") String until
    );
    );
}
