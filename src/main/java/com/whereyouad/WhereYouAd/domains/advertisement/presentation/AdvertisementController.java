package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.AdvertisementQueryService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.AdvertisementCommandService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.AdvertisementControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/advertisement")
public class AdvertisementController implements AdvertisementControllerDocs {

    private final AdvertisementQueryService advertisementQueryService;
    private final AdvertisementCommandService advertisementCommandService;

    // AdContent - 개별 광고 상세 조회
    @GetMapping("/{orgId}/projects/{projectId}/ad-contents/{adContentId}")
    public ResponseEntity<DataResponse<AdvertisementResponse.AdContentInfoResponse>> readAdContent(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long adContentId) {
        AdvertisementResponse.AdContentInfoResponse adContentInfoResponse = advertisementQueryService
                .readAdContent(userId, orgId, projectId, adContentId);
        return ResponseEntity.ok(DataResponse.from(adContentInfoResponse));
    }

    // AdContent - 캠페인(프로젝트) 내 광고 목록 조회
    @GetMapping("/{orgId}/projects/{projectId}/ad-contents")
    public ResponseEntity<DataResponse<AdvertisementResponse.AdContentInfosResponse>> readAdContents(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId, @PathVariable Long projectId) {
        AdvertisementResponse.AdContentInfosResponse adContentInfosResponse = advertisementQueryService
                .readAdContents(userId, orgId, projectId);
        return ResponseEntity.ok(DataResponse.from(adContentInfosResponse));
    }

    // AdGroup - 개별 광고의 광고 그룹(타켓팅 정보 등) 조회
    @GetMapping("/{orgId}/projects/{projectId}/ad-contents/{adContentId}/ad-group")
    public ResponseEntity<DataResponse<AdvertisementResponse.AdGroupInfoResponse>> readAdGroup(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long adContentId) {
        AdvertisementResponse.AdGroupInfoResponse adGroupInfoResponse = advertisementQueryService.readAdGroup(userId,
                orgId, projectId, adContentId);
        return ResponseEntity.ok(DataResponse.from(adGroupInfoResponse));
    }

    // Project - 단일 프로젝트(캠페인) 전체 중단/재개
    @PatchMapping("/{orgId}/projects/{projectId}/status")
    public ResponseEntity<DataResponse<Void>> updateProjectStatus(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId, @PathVariable Long projectId,
            @RequestParam Status status) {
        advertisementCommandService.updateProjectStatus(userId, orgId, projectId, status);
        return ResponseEntity.ok(DataResponse.ok());
    }

    // AdContent - 개별 광고 중단/재개
    @PatchMapping("/{orgId}/projects/{projectId}/ad-contents/{adContentId}/status")
    public ResponseEntity<DataResponse<Void>> updateAdContentStatus(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long adContentId,
            @RequestParam Status status) {
        advertisementCommandService.updateAdContentStatus(userId, orgId, projectId, adContentId, status);
        return ResponseEntity.ok(DataResponse.ok());
    }

}