package com.whereyouad.WhereYouAd.domains.click.presentation.docs;

import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

public interface ClickControllerDocs {

    @Operation(
            summary = "트래킹 링크 발급 API",
            description = "광고(adContentId)에 대한 트래킹 URL 발급\n\n" +
                    "- 하나의 광고당 URL은 1개만 존재\n" +
                    "- 이미 발급된 URL이 있으면 기존 URL을 그대로 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "성공 (trackingUrl 반환)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요 (토큰 없음 또는 만료)"),
            @ApiResponse(responseCode = "403", description = "해당 조직의 구성원이 아닌 경우"),
            @ApiResponse(responseCode = "404", description = "해당 adContentId의 광고가 존재하지 않는 경우")
    })
    ResponseEntity<DataResponse<ClickResponse.NewTrackingUrl>> createTrackingUrl(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long adContentId
    );
}
