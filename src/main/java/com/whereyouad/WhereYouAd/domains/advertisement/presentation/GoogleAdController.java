package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request.AdvertisementRequest;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.GoogleAdResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google.GoogleAdService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google.GoogleBudgetService;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.GoogleAdDocs;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/google")
public class GoogleAdController implements GoogleAdDocs {
    private final GoogleAdService googleAdService;
    private final GoogleBudgetService googleBudgetService;

    // API 호출하여 전체 캠페인, 광고 그룹, 개별 광고 및 MetricFact 조회 및 저장
    @Override
    @PostMapping("/ad-infos")
    public ResponseEntity<DataResponse<GoogleAdResponse.GoogleAdCreateResponse>> createAllAdInfos(@AuthenticationPrincipal(expression = "userId") Long userId) {
        GoogleAdResponse.GoogleAdCreateResponse googleAdCreateResponse = googleAdService.createAllAdInfos(userId);
        return ResponseEntity.ok(DataResponse.from(googleAdCreateResponse));
    }

    @Override
    @PatchMapping("/campaigns/{adCampaignId}/budget")
    public ResponseEntity<DataResponse<GoogleAdResponse.BudgetUpdateResponse>> updateCampaignBudget(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long adCampaignId,
            @RequestBody @Valid AdvertisementRequest.GoogleBudgetUpdateRequest request) {
        
        GoogleAdResponse.BudgetUpdateResponse response = googleBudgetService.updateCampaignBudget(userId, adCampaignId, request);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
