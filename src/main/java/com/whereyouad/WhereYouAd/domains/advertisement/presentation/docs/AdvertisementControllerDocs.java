package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Advertisement API", description = "광고 조회, 상태 변경 관련 API")
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

        @Operation(summary = "단일 프로젝트(캠페인) 전체 중단/재개", description = "특정 프로젝트에 속한 모든 광고(AdCampaign, AdGroup, AdContent)의 상태를 일괄적으로 중단(PAUSED) 시키거나 재개(ON_GOING)합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "403", description = "AD_403_1: 해당 프로젝트에 대한 접근 권한이 없습니다."),
                        @ApiResponse(responseCode = "404", description = "ORG_404_2: 해당 멤버가 조직에 존재하지 않습니다.<br>AD_404_1: 해당 프로젝트가 존재하지 않음")
        })
        ResponseEntity<DataResponse<Void>> updateProjectStatus(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @PathVariable Long orgId, @PathVariable Long projectId,
                        @RequestParam Status status);

        @Operation(summary = "개별 광고 중단/재개", description = "특정 개별 광고 콘텐츠(AdContent)의 상태를 중단(PAUSED) 시키거나 재개(ON_GOING)합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "성공"),
                        @ApiResponse(responseCode = "404", description = "ORG_404_2: 해당 멤버가 조직에 존재하지 않습니다.<br>AD_404_3: 해당 광고가 존재하지 않음")
        })
        ResponseEntity<DataResponse<Void>> updateAdContentStatus(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long adContentId,
                        @RequestParam Status status);


        @Operation(
                summary = "각 플랫폼별 광고 캠페인 조회 API",
                description = "각 플랫폼별로 우리 서비스 내에서 캠페인 그룹으로 묶이지 않은 캠페인을 조회합니다.\n\n" +
                        "/api/project/create/{orgId} 캠페인 그룹 정보 설정 API 에서 각 플랫폼별 캠페인을 선택하기 위해 사용되는 API 입니다."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "성공"),
                @ApiResponse(responseCode = "404", description = "ORG_404_2: 해당 멤버가 조직에 존재하지 않습니다."),
                @ApiResponse(responseCode = "400", description = "AD_400_2: providerType 입력이 잘못되었습니다."),
                @ApiResponse(responseCode = "404", description = "ORG_404_1: 해당 id 의 조직이 존재하지 않습니다.")
        })
        public ResponseEntity<DataResponse<AdvertisementResponse.AdCampaignListResponse>> readAdCampaigns(
                @AuthenticationPrincipal(expression = "userId") Long userId,
                @PathVariable Long orgId,
                @RequestParam(required = true) String providerType
        );
}
