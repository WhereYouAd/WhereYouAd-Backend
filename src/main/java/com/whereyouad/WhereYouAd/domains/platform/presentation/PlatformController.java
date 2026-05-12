package com.whereyouad.WhereYouAd.domains.platform.presentation;

import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;
import com.whereyouad.WhereYouAd.domains.platform.domain.service.PlatformService;
import com.whereyouad.WhereYouAd.domains.platform.presentation.docs.PlatformControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/platform")
public class PlatformController implements PlatformControllerDocs {

    private final PlatformService platformService;

    //네이버 광고 API 최초 등록 시 사용 (기존 코드 유지)
    @PostMapping("/{orgId}/accounts/naver")
    public ResponseEntity<DataResponse<PlatformResponse.PlatformAccount>> addNaverAdAccount(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody PlatformRequest.PlatformAccount dto
    ) {
        PlatformResponse.PlatformAccount response =
                platformService.addNaverAdAccount(userId, orgId, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.created(response));
    }

    @GetMapping("/{orgId}/accounts")
    public ResponseEntity<DataResponse<PlatformResponse.PlatformAccountListResponse>> getPlatformSyncInfos(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    )
    {
        PlatformResponse.PlatformAccountListResponse response = platformService.getPlatformSyncInfos(userId, orgId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    // 네이버 광고 API 수정 시 사용
    // 네이버 제외 구글, 메타의 경우 기존 OAuth 로그인 API 를 재호출 하도록 하여 수정 진행
    @PatchMapping("/{orgId}/accounts/naver")
    public ResponseEntity<DataResponse<PlatformResponse.PlatformAccount>> updateNaverAdAccount(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody PlatformRequest.UpdateNaverApiRequest request
    )
    {
        PlatformResponse.PlatformAccount response = platformService.updateNaverAdAccount(userId, orgId, request);
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

}
