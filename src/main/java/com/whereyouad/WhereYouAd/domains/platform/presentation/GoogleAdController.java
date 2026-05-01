package com.whereyouad.WhereYouAd.domains.platform.presentation;

import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.GoogleAdResponse;
import com.whereyouad.WhereYouAd.domains.platform.domain.service.GoogleAdService;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/google")
public class GoogleAdController {

    private final GoogleAdService googleAdService;

    // API 호출하여 전체 캠페인, 광고 그룹, 개별 광고 및 MetricFact 조회 및 저장
    @PostMapping("/ad-infos")
    public ResponseEntity<DataResponse<GoogleAdResponse.GoogleAdCreateReponse>> createAllAdInfos(@AuthenticationPrincipal(expression = "userId") Long userId) {
        GoogleAdResponse.GoogleAdCreateReponse googleAdCreateReponse = googleAdService.createAllAdInfos(userId);
        return ResponseEntity.ok(DataResponse.from(googleAdCreateReponse));
    }
}
