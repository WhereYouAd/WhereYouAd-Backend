package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface NaverAdApiControllerDocs {

    @Operation(summary = "네이버 광고 캠페인 목록 조회", description = "연동된 네이버 광고 계정의 캠페인 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캠페인 목록 반환"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 네이버 연결 정보 없음"),
            @ApiResponse(responseCode = "500", description = "네이버 API 호출 실패")
    })
    ResponseEntity<DataResponse<List<NaverDTO.Campaign>>> getCampaigns(
            @Parameter(description = "조직 ID", example = "1", required = true)
            @PathVariable Long orgId
    );
}
