package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.NaverAdApiService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.NaverAdApiControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/naver/{orgId}")
public class NaverAdApiController implements NaverAdApiControllerDocs {

    private final NaverAdApiService naverAdApiService;

    // 캠페인 목록 조회
    @GetMapping("/campaigns")
    public ResponseEntity<DataResponse<List<NaverDTO.Campaign>>> getCampaigns(
            @PathVariable Long orgId
    ) {
        return ResponseEntity.ok(DataResponse.from(naverAdApiService.getCampaigns(orgId)));
    }
}
