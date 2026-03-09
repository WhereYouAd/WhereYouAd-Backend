package com.whereyouad.WhereYouAd.infrastructure.client.openai.prompt;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    // 시스템 프롬프트 (AI 역할 지정 + 응답 형식)
    public String buildSystemPrompt() {
        return """
               당신은 디지털 광고 성과 분석 전문가입니다.
               사용자가 제공하는 일별 광고 성과 데이터를 기반으로 자연어 리포트를 작성해주세요.

               반드시 아래 JSON 형식으로만 응답하세요. JSON 이외의 설명이나 마크다운 코드블록은 포함하지 마세요.

               {
                 "strategySuggestion": "전략 제안 (단일 문자열)",
                 "performanceSummary": "성과 요약 (단일 문자열)",
                 "analysisReason": "왜 이렇게 나왔을까? (단일 문자열)",
                 "performancePoint": ["성과 포인트 1", "성과 포인트 2", "성과 포인트 3"],
                 "cautionPoint": ["주의 사항 1", "주의 사항 2"]
               }

               분석 항목 가이드:
               - strategySuggestion: 예산 소진율을 고려한 데이터 기반의 구체적인 광고 운영 전략 제안
               - performanceSummary: 전체 광고 성과 및 예산 소진 현황에 대한 객관적 요약
               - analysisReason: 성과가 좋거나 나쁜 원인 분석 (트렌드, 피크일, 부진일, 예산 영향 포함)
               - performancePoint: 성과가 좋은 날짜/구간/지표 (리스트)
               - cautionPoint: 개선이 필요하거나 주의가 필요한 날짜/구간/예산 초과 위험 (리스트)
               """;
    }

    // 유저 프롬프트 생성
    // Section 1: 캠페인별 예산 vs 실제 소진액 요약
    // Section 2: 일별 MetricFact 원본 CSV
    public String buildUserPrompt(LocalDate startDate, LocalDate endDate, List<MetricFact> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "아래는 %s ~ %s 기간 동안의 광고 성과 원본 데이터입니다.\n" +
                "데이터를 분석하여 지정된 JSON 형식으로 리포트를 작성해주세요.\n\n",
                startDate, endDate));

        // Section 1: 캠페인별 예산 소진 현황
        sb.append("[캠페인 예산 소진 현황]\n");
        sb.append("CampaignId,Budget,TotalSpend,UsageRate(%)\n");

        // MetricFact 목록에서 캠페인별 spend 합산
        Map<Long, CampaignBudgetAccumulator> budgetMap = new LinkedHashMap<>();
        for (MetricFact m : metrics) {
            AdCampaign camp = m.getAdContent().getAdGroup().getAdCampaign();
            Long campId = camp.getId();
            BigDecimal spend = m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO;
            Long budget = camp.getBudget();

            budgetMap.computeIfAbsent(campId, id -> new CampaignBudgetAccumulator(id, budget))
                    .addSpend(spend);
        }

        for (CampaignBudgetAccumulator acc : budgetMap.values()) {
            String usageRate = acc.budget == null || acc.budget == 0
                    ? "N/A"
                    : String.format("%.1f", acc.totalSpend.doubleValue() / acc.budget * 100);
            String budgetStr = acc.budget != null ? acc.budget.toString() : "N/A";
            sb.append(String.format("%d,%s,%s,%s\n",
                    acc.campaignId, budgetStr, acc.totalSpend.toPlainString(), usageRate));
        }

        sb.append("\n");

        // Section 2: 일별 MetricFact 원본 CSV
        sb.append("[일별 광고 성과 원본 데이터]\n");
        // 파생 지표(CTR, CVR, ROAS)는 AI가 추론하도록 생략하여 토큰 절약
        sb.append("Date,Provider,CampaignId,Imp,Clk,Conv,Spend,Rev\n");

        for (MetricFact m : metrics) {
            String date = m.getTimeBucket() != null ? m.getTimeBucket().toLocalDate().toString() : "";
            String provider = m.getProvider() != null ? m.getProvider().name() : "UNKN";
            String campId = m.getAdContent().getAdGroup().getAdCampaign().getId() != null
                    ? m.getAdContent().getAdGroup().getAdCampaign().getId().toString()
                    : "-";

            long imp = m.getImpressions() != null ? m.getImpressions() : 0L;
            long clk = m.getClicks() != null ? m.getClicks() : 0L;
            long conv = m.getConversions() != null ? m.getConversions() : 0L;
            String spend = m.getSpend() != null ? m.getSpend().toPlainString() : "0";
            String rev = m.getRevenue() != null ? m.getRevenue().toPlainString() : "0";

            sb.append(String.format("%s,%s,%s,%d,%d,%d,%s,%s\n",
                    date, provider, campId, imp, clk, conv, spend, rev));
        }

        sb.append("""
                  
                  위 원본 데이터를 바탕으로:
                  1. 기간 전체 성과 종합 평가
                  2. 성과 우수/부진 일자 파악
                  3. 플랫폼 및 캠페인별 성과 차이 분석
                  4. 주요 지표(CTR, CVR, ROAS) 추이 변화 분석
                  5. 예산 소진율을 반영한 구체적인 운영 전략 제안
                  """);

        return sb.toString();
    }

    // 캠페인별 예산 집계용 클래스
    private static class CampaignBudgetAccumulator {
        final Long campaignId;
        final Long budget;
        BigDecimal totalSpend = BigDecimal.ZERO;

        CampaignBudgetAccumulator(Long campaignId, Long budget) {
            this.campaignId = campaignId;
            this.budget = budget;
        }

        void addSpend(BigDecimal spend) {
            this.totalSpend = this.totalSpend.add(spend);
        }
    }
}