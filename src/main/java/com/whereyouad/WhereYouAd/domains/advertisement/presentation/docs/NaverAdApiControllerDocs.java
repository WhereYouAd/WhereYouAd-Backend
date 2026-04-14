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
    ResponseEntity<DataResponse<List<NaverDTO.Campaign>>> getCampaigns(
            @Parameter(description = "조직 ID", example = "1", required = true)
            @PathVariable Long orgId
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
    );
}
