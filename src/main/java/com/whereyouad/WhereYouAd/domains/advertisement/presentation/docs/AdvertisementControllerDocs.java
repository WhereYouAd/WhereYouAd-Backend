package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdvertisementControllerDocs {

        @Operation(summary = "캠페인(프로젝트) 내 개별 광고 조회", description = "캠페인(세 플랫폼의 캠페인을 합친 프로젝트) 내의 개별 광고에 대한 상세 정보(해당하는 광고 그룹 타겟 정보까지 포함)를 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "404", description = "ORG_404_2: 해당 멤버가 조직에 존재하지 않습니다. <br>AD_404_1: 해당 캠페인이 존재하지 않음 <br>AD_404_2: 해당 광고 그룹이 존재하지 않음 <br>AD_404_3: 해당 광고가 존재하지 않음")
        })
        ResponseEntity<DataResponse<AdvertisementResponse.AdContentInfoResponse>> readAdContent(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long adContentId);

        @Operation(summary = "캠페인(프로젝트) 내 광고 목록 조회", description = "캠페인(세 플랫폼의 캠페인을 합친 프로젝트) 내의 광고 목록을 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "404", description = "ORG_404_2: 해당 멤버가 조직에 존재하지 않습니다.")
        })
        ResponseEntity<DataResponse<AdvertisementResponse.AdContentInfosResponse>> readAdContents(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @PathVariable Long orgId, @PathVariable Long projectId);

        @Operation(summary = "캠페인(프로젝트) 내 특정 광고의 광고 그룹 조회", description = "캠페인(세 플랫폼의 캠페인을 합친 프로젝트) 내의 특정 광고의 광고 그룹 정보를 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "404", description = "ORG_404_2: 해당 멤버가 조직에 존재하지 않습니다.<br>AD_404_1: 해당 캠페인이 존재하지 않음 <br>AD_404_2: 해당 광고 그룹이 존재하지 않음 <br>AD_404_3: 해당 광고가 존재하지 않음")
        })
        ResponseEntity<DataResponse<AdvertisementResponse.AdGroupInfoResponse>> readAdGroup(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long adContentId);

}
