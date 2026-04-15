package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.NaverAdApiService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.NaverAdSyncService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.NaverAdApiControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/naver/{connectionId}")
public class NaverAdApiController implements NaverAdApiControllerDocs {

    private final NaverAdApiService naverAdApiService;
    private final NaverAdSyncService naverAdSyncService;

    // 캠페인 목록 조회
    @GetMapping("/campaigns")
    public ResponseEntity<DataResponse<List<NaverDTO.CampaignResponse>>> getCampaigns(
            @PathVariable Long connectionId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getCampaigns(connectionId)));
    }

    // 광고 그룹 목록 조회
    @GetMapping("/adgroups")
    public ResponseEntity<DataResponse<List<NaverDTO.AdGroupResponse>>> getAdGroups(
            @PathVariable Long connectionId,
            @RequestParam("nccCampaignId") String nccCampaignId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getAdGroups(connectionId, nccCampaignId)));
    }

    // 광고 소재 목록 조회
    @GetMapping("/ads")
    public ResponseEntity<DataResponse<List<NaverDTO.AdResponse>>> getAds(
            @PathVariable Long connectionId,
            @RequestParam("nccAdgroupId") String nccAdgroupId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getAds(connectionId, nccAdgroupId)));
    }

    // 키워드 목록 조회
    @GetMapping("/keywords")
    public ResponseEntity<DataResponse<List<NaverDTO.KeywordResponse>>> getKeywords(
            @PathVariable Long connectionId,
            @RequestParam("nccAdgroupId") String nccAdgroupId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getKeywords(connectionId, nccAdgroupId)));
    }

    // AD 리포트 생성 요청
    @PostMapping("/reports/ad")
    public ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> requestAdReport(
            @PathVariable Long connectionId,
            @RequestParam("statDt") String statDt
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.requestAdReport(connectionId, statDt)));
    }

    // AD_CONVERSION 리포트 생성 요청
    @PostMapping("/reports/ad-conversion")
    public ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> requestAdConversionReport(
            @PathVariable Long connectionId,
            @RequestParam("statDt") String statDt
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.requestAdConversionReport(connectionId, statDt)));
    }

    // 보고서 상태 조회
    @GetMapping("/reports/{reportJobId}")
    public ResponseEntity<DataResponse<NaverDTO.StatReportResponse>> getReportStatus(
            @PathVariable Long connectionId,
            @PathVariable String reportJobId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getReportStatus(connectionId, reportJobId)));
    }

    // 보고서 다운로드
    @GetMapping("/reports/download")
    public ResponseEntity<DataResponse<NaverDTO.RawReportResponse>> downloadReport(
            @PathVariable Long connectionId,
            @RequestParam("url") String downloadUrl
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getCampaigns(orgId)));
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.downloadReport(connectionId, downloadUrl)));
    }
    }
}
