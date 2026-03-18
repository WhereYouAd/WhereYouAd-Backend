package com.whereyouad.WhereYouAd.domains.click.presentation.docs;

import com.whereyouad.WhereYouAd.domains.click.application.dto.request.ClickRequest;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ClickControllerDocs {

    @Operation(
            summary = "트래킹 링크 발급 API",
            description = "광고(adContentId)에 대한 트래킹 URL 발급\n\n" +
                    "- 하나의 광고당 URL은 1개만 존재\n" +
                    "- 이미 발급된 URL이 있으면 기존 URL을 그대로 반환" +
                    "- 트래킹 링크를 발급하기 위해서 landingURL이 있어야 정상적으로 redirect 되므로 필수적으로 입력"
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
            @PathVariable Long adContentId,
            @Valid @RequestBody ClickRequest.CreateTrackingUrl request
    );
    @Operation(
            summary = "광고 트래킹 리다이렉트 API",
            description = "발급된 트래킹 링크 접속 시 클릭 이벤트 기록 후 광고 페이지로 리다이렉트"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "리다이렉트 성공"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 트래킹 링크")
    })
    ResponseEntity<Void> processTracking(
            @PathVariable String code,
            HttpServletRequest request
    );
}
