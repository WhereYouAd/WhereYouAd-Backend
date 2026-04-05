package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.MetaAdApiService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.MetaAdApiControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meta")
public class MetaAdApiController implements MetaAdApiControllerDocs {

    private final MetaAdApiService metaAdApiService;

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

    // 2. OAuth 콜백 → 토큰 발급 + 즉시 전체 동기화
    @GetMapping("/callback")
    public ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> callback(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam String code,
            @RequestParam(name = "state") Long orgId) // state에서 orgId 값 수신
    {
        MetaResponse.MetaSyncSummary response = metaAdApiService.handleCallback(orgId, userId, code);
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    // 3. 수동 동기화 트리거 (관리자용)
    @PostMapping("/{orgId}/sync")
    public ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> syncManually(
            @PathVariable Long orgId,
            @RequestBody @Valid MetaRequest.MetaManualSyncRequest request
    )
    {

        MetaResponse.MetaSyncSummary response = metaAdApiService.syncAll(orgId, request.startDate(), request.endDate());
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }
}
