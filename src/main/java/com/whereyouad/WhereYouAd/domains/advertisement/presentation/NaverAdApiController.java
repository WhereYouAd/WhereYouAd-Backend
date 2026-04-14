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
    public ResponseEntity<DataResponse<List<NaverDTO.Campaign>>> getCampaigns(
            @PathVariable Long orgId
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
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getCampaigns(orgId)));
    }
}
