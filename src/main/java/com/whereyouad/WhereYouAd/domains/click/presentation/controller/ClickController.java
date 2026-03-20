package com.whereyouad.WhereYouAd.domains.click.presentation.controller;

import com.whereyouad.WhereYouAd.domains.click.application.dto.request.ClickRequest;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickService;
import com.whereyouad.WhereYouAd.domains.click.presentation.docs.ClickControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/clicks")
@RequiredArgsConstructor
public class ClickController implements ClickControllerDocs {

    private final ClickService clickService;

    @PostMapping("/{orgId}/{adContentId}/tracking-url")
    public ResponseEntity<DataResponse<ClickResponse.NewTrackingUrl>> createTrackingUrl(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long adContentId,
            @Valid @RequestBody ClickRequest.CreateTrackingUrl request
    ) {
        ClickResponse.NewTrackingUrl response = clickService.createTrackingUrl(userId, adContentId, orgId, request.landingUrl());
        return ResponseEntity.ok(
                DataResponse.created(response)
        );
    }

    @Override
    @GetMapping("/track/{code}")
    public ResponseEntity<Void> processTracking(
            @PathVariable String code,
            HttpServletRequest request
    ) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "Unknown";

        String landingUrl = clickService.handleTrackingRedirect(code, ipAddress, userAgent);

        HttpHeaders headers = new HttpHeaders();
        // 헤더 location에 랜딩 url 삽입
        headers.setLocation(URI.create(landingUrl));
        // 302 리다이렉트
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // (임시) 실시간 클릭수 조회 (dummy 또는 실제 실시간 집계)
    // GET /api/clicks/realtime/{adContentId}?mode=real&minutes=60
    @GetMapping("/realtime/{adContentId}")
    public ResponseEntity<DataResponse<java.util.List<ClickResponse.RealtimeClickCount>>> getRealtimeClickCounts(
            @PathVariable Long adContentId,
            @RequestParam(defaultValue = "real") String mode,
            @RequestParam(defaultValue = "60") int minutes
    ) {
        return ResponseEntity.ok(
                DataResponse.from(clickService.getRealtimeClickCounts(adContentId, mode, minutes))
        );
    }
}
