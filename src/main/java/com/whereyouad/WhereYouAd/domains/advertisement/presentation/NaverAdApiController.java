package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request.AdvertisementRequest;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.NaverAdApiService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.NaverAdSyncService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.NaverAdApiControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/naver")
public class NaverAdApiController implements NaverAdApiControllerDocs {

    private final NaverAdApiService naverAdApiService;
    private final NaverAdSyncService naverAdSyncService;

    // 캠페인 목록 조회
    @GetMapping("/{connectionId}/campaigns")
    public ResponseEntity<DataResponse<List<NaverDTO.CampaignResponse>>> getCampaigns(
            @PathVariable Long connectionId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getCampaigns(connectionId)));
    }

    // 광고 그룹 목록 조회
    @GetMapping("/{connectionId}/adgroups")
    public ResponseEntity<DataResponse<List<NaverDTO.AdGroupResponse>>> getAdGroups(
            @PathVariable Long connectionId,
            @RequestParam("nccCampaignId") String nccCampaignId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getAdGroups(connectionId, nccCampaignId)));
    }

    // 광고 소재 목록 조회
    @GetMapping("/{connectionId}/ads")
    public ResponseEntity<DataResponse<List<NaverDTO.AdResponse>>> getAds(
            @PathVariable Long connectionId,
            @RequestParam("nccAdgroupId") String nccAdgroupId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getAds(connectionId, nccAdgroupId)));
    }

    // 키워드 목록 조회
    @GetMapping("/{connectionId}/keywords")
    public ResponseEntity<DataResponse<List<NaverDTO.KeywordResponse>>> getKeywords(
            @PathVariable Long connectionId,
            @RequestParam("nccAdgroupId") String nccAdgroupId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getKeywords(connectionId, nccAdgroupId)));
    }

    // AD 리포트 생성 요청
    @PostMapping("/{connectionId}/reports/ad")
    public ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> requestAdReport(
            @PathVariable Long connectionId,
            @RequestParam("statDt") String statDt
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.requestAdReport(connectionId, statDt)));
    }

    // AD_CONVERSION 리포트 생성 요청
    @PostMapping("/{connectionId}/reports/ad-conversion")
    public ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> requestAdConversionReport(
            @PathVariable Long connectionId,
            @RequestParam("statDt") String statDt
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.requestAdConversionReport(connectionId, statDt)));
    }

    // 보고서 상태 조회
    @GetMapping("/{connectionId}/reports/{reportJobId}")
    public ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> getReportStatus(
            @PathVariable Long connectionId,
            @PathVariable String reportJobId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getReportStatus(connectionId, reportJobId)));
    }

    // 보고서 다운로드
    @GetMapping("/{connectionId}/reports/download")
    public ResponseEntity<DataResponse<NaverDTO.RawReportResponse>> downloadReport(
            @PathVariable Long connectionId,
            @RequestParam("url") String downloadUrl
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.downloadReport(connectionId, downloadUrl)));
    }

    // 일별 통계 직접 조회 (테스트용)
    @GetMapping("/{connectionId}/stats/daily")
    public ResponseEntity<DataResponse<List<NaverDTO.StatResponse>>> getDailyStats(
            @PathVariable Long connectionId,
            @RequestParam("id") String id,
            @RequestParam("since") String since,
            @RequestParam("until") String until
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getDailyStats(connectionId, id, since, until)));
    }

    // 메타데이터 동기화 (캠페인/그룹/소재)
    @PostMapping("/{connectionId}/sync/metadata")
    public ResponseEntity<DataResponse<AdvertisementResponse.NaverMetadataSyncResponse>> syncMetadata(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long connectionId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdSyncService.syncAllMetadata(userId, connectionId)));
    }

    // 수동 동기화 (orgId + 날짜 범위)
    @PostMapping("/organizations/{orgId}/sync")
    public ResponseEntity<DataResponse<AdvertisementResponse.NaverManualSyncSummary>> syncManually(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid AdvertisementRequest.ManualSyncRequest request
    ) {
        return ResponseEntity.ok(DataResponse.from(
                naverAdSyncService.syncAllForOrg(userId, orgId, request.startDate(), request.endDate())));
    }

    // DAILY MetricFact 동기화 (기본 지표 + 전환 지표 통합)
    @PostMapping("/{connectionId}/sync/stats")
    public ResponseEntity<DataResponse<AdvertisementResponse.NaverStatSyncResponse>> syncStats(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long connectionId,
            @RequestParam("statDate") String statDate
    ) {
        return ResponseEntity.ok(DataResponse.from(
                naverAdSyncService.syncBasicStats(userId, connectionId, statDate)));
    }

    // 캠페인 예산 수정
    @PutMapping("/{connectionId}/campaigns/{campaignId}/budget")
    public ResponseEntity<DataResponse<NaverDTO.CampaignResponse>> updateCampaignBudget(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long connectionId,
            @PathVariable String campaignId,
            @RequestBody NaverDTO.UpdateCampaignBudgetRequest request
    ) {
        return ResponseEntity.ok(DataResponse.from(
                naverAdApiService.updateCampaignBudget(userId, connectionId, campaignId, request)));
    }

    // 광고그룹 예산 수정
    @PutMapping("/{connectionId}/adgroups/{adgroupId}/budget")
    public ResponseEntity<DataResponse<NaverDTO.AdGroupResponse>> updateAdGroupBudget(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long connectionId,
            @PathVariable String adgroupId,
            @RequestBody NaverDTO.UpdateAdGroupBudgetRequest request
    ) {
        return ResponseEntity.ok(DataResponse.from(
                naverAdApiService.updateAdGroupBudget(userId, connectionId, adgroupId, request)));
    }
}
