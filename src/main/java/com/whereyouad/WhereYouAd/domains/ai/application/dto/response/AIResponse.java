package com.whereyouad.WhereYouAd.domains.ai.application.dto.response;

import lombok.Builder;

import java.util.List;

public class AIResponse {

    // 광고 성과 분석 API 응답
    @Builder
    public record AnalysisResponse(
            // 전략 제안: AI가 제안하는 광고 운영 전략
            String strategySuggestion,

            // 성과 요약: 기간 전체 광고 성과 요약
            String performanceSummary,

            // 분석 근거: 해당 성과가 나온 이유 분석
            String analysisReason,

            // 성과 포인트: 잘 된 부분 목록
            List<String> performancePoint,

            // 주의 포인트: 개선이 필요한 부분 목록
            List<String> cautionPoint
    ) {}

    // 분석 결과 요청 응답 DTO (result는 PENDING/FAILED일 때 null)
    public record ReportStatusResponse(
            String accessToken,
            String status,
            AnalysisResponse result // PENDING/FAILED 일 때 null
    ) {}
}
