package com.whereyouad.WhereYouAd.domains.click.presentation;

import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickService;
import com.whereyouad.WhereYouAd.domains.click.presentation.docs.ClickControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clicks")
@RequiredArgsConstructor
public class ClickController implements ClickControllerDocs {

    private final ClickService clickService;

    @PostMapping("/{orgId}/{adContentId}/tracking-url")
    public ResponseEntity<DataResponse<ClickResponse.NewTrackingUrl>> createTrackingUrl(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long adContentId
    ) {
        ClickResponse.NewTrackingUrl response = clickService.createTrackingUrl(userId, adContentId, orgId);
        return ResponseEntity.ok(
                DataResponse.created(response)
        );
    }
}
