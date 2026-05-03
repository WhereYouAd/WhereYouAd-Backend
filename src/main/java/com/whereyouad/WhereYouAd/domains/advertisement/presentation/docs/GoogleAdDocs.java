package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.GoogleAdResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Google Ad 연동 API", description = "구글 광고 데이터 조회 및 동기화 관련 API")
public interface GoogleAdDocs {

    @Operation(
            summary = "구글 광고 데이터 수동 연동",
            description = "인증된 사용자의 플랫폼 연동 정보를 바탕으로 구글 광고 API를 호출하여 전체 캠페인, 광고 그룹, 개별 광고 소재 및 통계 데이터(MetricFact)를 조회하고 데이터베이스에 동기화(Upsert)합니다."
    )
    @PostMapping("/ad-infos")
    ResponseEntity<DataResponse<GoogleAdResponse.GoogleAdCreateReponse>> createAllAdInfos(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userId") Long userId);
}
