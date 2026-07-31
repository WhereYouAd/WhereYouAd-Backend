package com.whereyouad.WhereYouAd.domains.ai.presentation;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.request.AIRequest;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.domains.ai.domain.service.AIService;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.domains.ai.presentation.docs.AIControllerDocs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AIController implements AIControllerDocs {

    private final AIService aiService;

    @PostMapping("/organizations/{orgId}/analysis")
    public ResponseEntity<DataResponse<String>> requestAnalysis(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid AIRequest.AnalysisRequest request) {
        String accessToken = aiService.requestAnalysis(userId, orgId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DataResponse.from(accessToken));
    }

    @GetMapping("/reports/{accessToken}")
    public ResponseEntity<DataResponse<AIResponse.ReportStatusResponse>> getReportByAccessToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String accessToken) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        AIResponse.ReportStatusResponse response = aiService.getReportByAccessToken(userId, accessToken);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/organizations/{orgId}/reports")
    public ResponseEntity<DataResponse<AIResponse.ReportListResponse>> getReportSummaries(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        AIResponse.ReportListResponse response =
                aiService.getReportSummaries(userId, orgId, reportType, cursor, size);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PatchMapping("/reports/{accessToken}/share")
    public ResponseEntity<DataResponse<String>> updateShareStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String accessToken,
            @RequestParam boolean isShared) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        aiService.updateShareStatus(userId, accessToken, isShared);
        return ResponseEntity.ok(DataResponse.from("SUCCESS"));
    }
}
