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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/platform")
public class PlatformController implements PlatformControllerDocs {

    private final PlatformService platformService;

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
}
