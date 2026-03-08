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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AIController implements AIControllerDocs {

    private final AIService aiService;

    @PostMapping("/{orgId}/analysis")
    public ResponseEntity<DataResponse<Long>> requestAnalysis(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid AIRequest.PeriodRequest request) {
        Long reportId = aiService.requestAnalysis(userId, orgId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DataResponse.from(reportId));
    }

    @GetMapping("/{orgId}/analysis/{reportId}")
    public ResponseEntity<DataResponse<AIResponse.ReportStatusResponse>> getReport(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long reportId) {
        AIResponse.ReportStatusResponse response = aiService.getReport(userId, orgId, reportId);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
