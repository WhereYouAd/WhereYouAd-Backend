package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.MetaAdApiService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.MetaAdApiControllerDocs;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.config.MetaAdConfig;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meta")
public class MetaAdApiController implements MetaAdApiControllerDocs {

    private final MetaAdApiService metaAdApiService;
    private final MetaAdConfig metaAdConfig;

    // 1. OAuth 인증 URL 반환
    @GetMapping("/auth-url")
    public ResponseEntity<DataResponse<MetaResponse.AuthUrlResponse>> getAuthUrl(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam Long orgId)
    {

        MetaResponse.AuthUrlResponse response = metaAdApiService.getAuthorizationUrl(userId ,orgId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    // 2. OAuth 콜백 → 토큰 발급 + 즉시 전체 동기화 후 프론트로 302 리다이렉트
    // 사용자 브라우저가 직접 호출하는 엔드포인트이므로 JSON 대신 프론트 결과 페이지로 리다이렉트한다.
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription)
    {
        // 사용자가 Meta 동의 화면에서 거절
        if (error != null) {
            log.info("[META] OAuth 사용자 거부 - error={}, description={}", error, errorDescription);
            return redirectToFrontend("denied", error);
        }

        // 필수 파라미터 누락
        if (code == null || state == null) {
            log.warn("[META] OAuth callback 필수 파라미터 누락 - code={}, state={}", code != null, state != null);
            return redirectToFrontend("invalid_request", null);
        }

        try {
            MetaResponse.MetaSyncSummary summary = metaAdApiService.handleCallback(state, code);
            return redirectToFrontendWithSummary(summary);
        } catch (AppException e) {
            // 프론트에는 사용자가 직접 해결 가능한 케이스(Meta 광고 계정 없음)만 노출
            log.warn("[META] OAuth callback 처리 실패 - errorCode={}", e.getErrorCode().getCode());
            String detail = (e.getErrorCode() == AdApiErrorCode.NO_LINKABLE_AD_ACCOUNT)
                    ? "no_ad_account"
                    : "meta_oauth_failed"; //나머지는 전부 oauth 연동 실패 처리

            return redirectToFrontend("error", detail);
        } catch (Exception e) {
            log.error("[META] OAuth callback 처리 중 예외", e);
            return redirectToFrontend("error", "meta_oauth_failed");
        }
    }

    // 3. 수동 동기화 트리거 (디버깅, 테스팅 용도)
    @PostMapping("/{orgId}/sync")
    public ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> syncManually(
            @PathVariable Long orgId,
            @RequestBody @Valid MetaRequest.MetaManualSyncRequest request
    )
    {

        MetaResponse.MetaSyncSummary response = metaAdApiService.syncAll(orgId, request.startDate().toString(), request.endDate().toString());
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    // 4. 사용자 '갱신(refresh)' 요청 처리 API
    @PostMapping("/{orgId}/refresh")
    public ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> refreshForUser(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    )
    {
        MetaResponse.MetaSyncSummary response = metaAdApiService.refreshForUser(userId, orgId);
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    //===내부 편의 메서드===
    //콜백 실패시 프론트 리다이렉트 메서드
    private ResponseEntity<Void> redirectToFrontend(String status, String detail) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(metaAdConfig.getFrontendCallbackUrl())
                .queryParam("status", status);
        if (detail != null) {
            builder.queryParam("detail", detail);
        }
        URI location = builder.encode().build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    //콜백 성공시 성공한 갯수(캠페인, 그룹, 광고, 지표, 실패한 계정)와 함께 프론트 리다이렉트 메서드
    private ResponseEntity<Void> redirectToFrontendWithSummary(MetaResponse.MetaSyncSummary summary) {
        int failedCount = (summary.failedAccountIds() == null) ? 0 : summary.failedAccountIds().size();
        String status = (failedCount == 0) ? "success" : "partial";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(metaAdConfig.getFrontendCallbackUrl())
                .queryParam("status", status)
                .queryParam("adCampaigns", summary.adCampaignCount())
                .queryParam("adGroups", summary.adGroupCount())
                .queryParam("adContents", summary.adContentCount())
                .queryParam("metricFacts", summary.metricCount());

        if (failedCount > 0) {
            builder.queryParam("failedCount", failedCount);
        }

        URI location = builder.encode().build().toUri();

        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }
}
