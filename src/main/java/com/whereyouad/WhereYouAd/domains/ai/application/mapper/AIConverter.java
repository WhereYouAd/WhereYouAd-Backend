package com.whereyouad.WhereYouAd.domains.ai.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import com.whereyouad.WhereYouAd.domains.ai.domain.constant.AIStatus;
import com.whereyouad.WhereYouAd.domains.ai.exception.AIHandler;
import com.whereyouad.WhereYouAd.domains.ai.exception.code.AIErrorCode;
import com.whereyouad.WhereYouAd.domains.ai.persistence.entity.AIInsightReport;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.request.OpenAIRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

@Slf4j
public class AIConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 시스템 프롬프트와 유저 프롬프트를 받아 OpenAiRequest Feign 요청 DTO
    public static OpenAIRequest.Request toOpenAiRequest(
            String model, String systemPrompt, String userPrompt) {
        return toOpenAiRequest(model, systemPrompt, userPrompt, 0.7);
    }

    public static OpenAIRequest.Request toOpenAiRequest(
            String model, String systemPrompt, String userPrompt, double temperature) {
        return OpenAIRequest.Request.builder()
                .model(model)
                .messages(List.of(
                        OpenAIRequest.Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        OpenAIRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()))
                .temperature(temperature)
                .build();
    }

    // AI가 반환한 JSON 문자열을 AnalysisResponse DTO로 파싱 (OpenAI 응답 -> AnalysisResponse)
    public static AIResponse.AnalysisResponse toAnalysisResponse(String content) {
        try {
            // 마크다운 코드 블록 제거
            String cleaned = content
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);

            List<String> performancePoints = new ArrayList<>();
            node.path("performancePoint").forEach(n -> performancePoints.add(n.asText()));

            List<String> cautionPoints = new ArrayList<>();
            node.path("cautionPoint").forEach(n -> cautionPoints.add(n.asText()));

            return AIResponse.AnalysisResponse.builder()
                    .strategySuggestion(node.path("strategySuggestion").asText())
                    .performanceSummary(node.path("performanceSummary").asText())
                    .analysisReason(node.path("analysisReason").asText())
                    .performancePoint(performancePoints)
                    .cautionPoint(cautionPoints)
                    .build();

        } catch (Exception e) {
            log.error("[AIConverter] OpenAI 응답 JSON 파싱 실패: {}", e.getMessage());
            throw new AIHandler(AIErrorCode.AI_CALL_FAILED);
        }
    }

    public static WeeklyReportResponse.WeeklyAnalysisResponse toWeeklyAnalysisResponse(
            String content, WeeklyReportResponse.KpiOverview kpiOverview) {
        try {
            String cleaned = content
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleaned);

            List<WeeklyReportResponse.Highlight> highlights = new ArrayList<>();
            root.path("highlights").forEach(h -> highlights.add(
                    WeeklyReportResponse.Highlight.builder()
                            .type(h.path("type").asText())
                            .title(h.path("title").asText())
                            .what(h.path("what").asText())
                            .why(h.path("why").asText())
                            .evidence(h.path("evidence").asText())
                            .confidence(h.path("confidence").asText())
                            .action(h.path("action").asText())
                            .build()
            ));

            List<WeeklyReportResponse.PlatformInsight> platformInsights = new ArrayList<>();
            root.path("platformInsights").forEach(p -> platformInsights.add(
                    WeeklyReportResponse.PlatformInsight.builder()
                            .platform(p.path("platform").asText())
                            .oneLineSummary(p.path("oneLineSummary").asText())
                            .keyObservation(p.path("keyObservation").asText())
                            .build()
            ));

            List<String> dataCaveats = new ArrayList<>();
            root.path("dataCaveats").forEach(c -> dataCaveats.add(c.asText()));

            WeeklyReportResponse.NextWeekFocus nextWeekFocus = WeeklyReportResponse.NextWeekFocus.builder()
                    .focus(root.path("nextWeekFocus").path("focus").asText())
                    .reason(root.path("nextWeekFocus").path("reason").asText())
                    .expectedOutcome(root.path("nextWeekFocus").path("expectedOutcome").asText())
                    .build();

            return WeeklyReportResponse.WeeklyAnalysisResponse.builder()
                    .weekSummary(root.path("weekSummary").asText())
                    .kpiOverview(kpiOverview)
                    .highlights(highlights)
                    .platformInsights(platformInsights)
                    .dataCaveats(dataCaveats)
                    .nextWeekFocus(nextWeekFocus)
                    .build();

        } catch (Exception e) {
            log.error("[AIConverter] 주간 리포트 JSON 파싱 실패: {}", e.getMessage());
            throw new AIHandler(AIErrorCode.AI_CALL_FAILED);
        }
    }

    // AI 분석 요청 값 entity PENDING 상태로 저장
    public static AIInsightReport toAIInsightConverter(LocalDateTime start, LocalDateTime end, Organization organization, String reportType) {
        return AIInsightReport.builder()
                .organization(organization)
                .reportType(reportType)
                .periodStart(start)
                .periodEnd(end)
                .status(AIStatus.PENDING)
                .accessToken(UUID.randomUUID().toString())
                .isShared(false)
                .build();
    }
}
