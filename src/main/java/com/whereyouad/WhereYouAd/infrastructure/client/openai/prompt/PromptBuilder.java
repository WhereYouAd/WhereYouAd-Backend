package com.whereyouad.WhereYouAd.infrastructure.client.openai.prompt;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Goal;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    // 시스템 프롬프트 (AI 역할 지정 + 응답 형식)
    public String buildSystemPrompt() {
        return """
               당신은 디지털 광고 성과 분석 전문가입니다.
               사용자가 제공하는 선택한 플랫폼(또는 조직 전체)의 일별 광고 성과 데이터를 기반으로 날카롭고 실리적인 자연어 리포트를 작성해주세요.

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
    public String buildUserPrompt(String provider, LocalDate startDate, LocalDate endDate, List<MetricFact> metrics) {
        StringBuilder sb = new StringBuilder();
        String targetProvider = "ALL".equalsIgnoreCase(provider) ? "조직 전체 데이터" : provider + " 플랫폼";
        sb.append(String.format(
                "아래는 %s의 %s ~ %s 기간 동안의 광고 성과 원본 데이터입니다.\n" +
                "데이터를 분석하여 지정된 JSON 형식으로 리포트를 작성해주세요.\n\n",
                targetProvider, startDate, endDate));

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
            String platform = m.getProvider() != null ? m.getProvider().name() : "UNKN";
            String campId = m.getAdContent().getAdGroup().getAdCampaign().getId() != null
                    ? m.getAdContent().getAdGroup().getAdCampaign().getId().toString()
                    : "-";

            long imp = m.getImpressions() != null ? m.getImpressions() : 0L;
            long clk = m.getClicks() != null ? m.getClicks() : 0L;
            long conv = m.getConversions() != null ? m.getConversions() : 0L;
            String spend = m.getSpend() != null ? m.getSpend().toPlainString() : "0";
            String rev = m.getRevenue() != null ? m.getRevenue().toPlainString() : "0";

            sb.append(String.format("%s,%s,%s,%d,%d,%d,%s,%s\n",
                    date, platform, campId, imp, clk, conv, spend, rev));
        }

        sb.append("""
                  
                  위 원본 데이터를 바탕으로 이 특정 플랫폼(혹은 조직 대상) 데이터에 대한:
                  1. 기간 전체 성과 종합 평가
                  2. 성과 우수/부진 일자 파악
                  3. 플랫폼별 및 소속 캠페인 간의 성과 차이 분석
                  4. 주요 지표(CTR, CVR, ROAS) 추이 변화 분석
                  5. 예산 대비 소진율 및 프로젝트 목표를 고려한 실무적인 광고 운영 전략 제안
                  """);

        return sb.toString();
    }

    public String buildTimelineSystemPrompt() {
        return """
               당신은 디지털 광고 성과 타임라인 분석 전문가입니다.
               사용자가 제공하는 타임라인 기간의 일별 성과 데이터를 바탕으로 2~3문장의 자연스러운 한국어 요약문을 작성하세요.

               반드시 지켜야 할 규칙:
               - '활성화된 지표'에 명시된 지표만 분석하세요. 목록에 없는 지표는 절대 언급하지 마세요.
               - 데이터 부족, 추가 수집 필요, 알 수 없음 등의 표현은 절대 사용하지 마세요. 제공된 데이터만으로 분석하세요.
               - 전체적인 흐름, 특이점(최고/최저 날짜), 지표 간 변화를 포함하세요.
               - JSON, 마크다운, 불릿 포인트 없이 순수 텍스트로만 응답하세요.
               """;
    }

    public String buildTimelineUserPrompt(Timeline timeline, List<MetricFact> facts, List<MetricFact> comparisonFacts) {
        StringBuilder sb = new StringBuilder();

        List<String> activeMetricNames = new ArrayList<>();
        if (timeline.isUseClick()) activeMetricNames.add("클릭수");
        if (timeline.isUseConversion()) activeMetricNames.add("전환수");
        if (timeline.isUseImpression()) activeMetricNames.add("노출수");
        if (timeline.isUseRoas()) activeMetricNames.add("ROAS");

        sb.append(String.format("타임라인 이름: %s\n", timeline.getName()));
        sb.append(String.format("분석 기간: %s ~ %s\n", timeline.getStartDate(), timeline.getEndDate()));
        sb.append(String.format("비교 기간: %s ~ %s\n", timeline.getComparisonStartDate(), timeline.getComparisonEndDate()));
        if (timeline.getPerformanceStatus() != null) {
            String statusLabel = switch (timeline.getPerformanceStatus()) {
                case ABOVE_AVG -> "평균 이상";
                case ON_TRACK -> "보통 (목표 수준 유지)";
                case UNDERPERFORM -> "평균 이하";
            };
            sb.append(String.format("성과 상태: %s\n", statusLabel));
        }
        sb.append(String.format("활성화된 지표: %s\n\n", String.join(", ", activeMetricNames)));

        // 분석 기간 일별 데이터
        sb.append("분석 기간 일별 성과 데이터:\n");
        appendDailyMetrics(sb, timeline, facts, timeline.getStartDate(), timeline.getEndDate());

        // 비교 기간 일별 데이터
        sb.append("\n비교 기간 일별 성과 데이터:\n");
        appendDailyMetrics(sb, timeline, comparisonFacts, timeline.getComparisonStartDate(), timeline.getComparisonEndDate());

        return sb.toString();
    }

    private void appendDailyMetrics(StringBuilder sb, Timeline timeline, List<MetricFact> facts, LocalDate from, LocalDate to) {
        sb.append("Date");
        if (timeline.isUseClick()) sb.append(",Clk");
        if (timeline.isUseConversion()) sb.append(",Conv");
        if (timeline.isUseImpression()) sb.append(",Imp");
        if (timeline.isUseRoas()) sb.append(",ROAS");
        sb.append("\n");

        Map<LocalDate, List<MetricFact>> byDate = facts.stream()
                .collect(Collectors.groupingBy(f -> f.getTimeBucket().toLocalDate()));

        from.datesUntil(to.plusDays(1)).forEach(date -> {
            List<MetricFact> dayFacts = byDate.getOrDefault(date, List.of());
            sb.append(date);

            if (timeline.isUseClick()) {
                long clk = dayFacts.stream().mapToLong(f -> f.getClicks() != null ? f.getClicks() : 0L).sum();
                sb.append(",").append(clk);
            }
            if (timeline.isUseConversion()) {
                long conv = dayFacts.stream().mapToLong(f -> f.getConversions() != null ? f.getConversions() : 0L).sum();
                sb.append(",").append(conv);
            }
            if (timeline.isUseImpression()) {
                long imp = dayFacts.stream().mapToLong(f -> f.getImpressions() != null ? f.getImpressions() : 0L).sum();
                sb.append(",").append(imp);
            }
            if (timeline.isUseRoas()) {
                BigDecimal spend = dayFacts.stream()
                        .map(f -> f.getSpend() != null ? f.getSpend() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal revenue = dayFacts.stream()
                        .map(f -> f.getRevenue() != null ? f.getRevenue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String roasStr = spend.compareTo(BigDecimal.ZERO) > 0
                        ? revenue.divide(spend, 2, RoundingMode.HALF_UP).toPlainString()
                        : "0";
                sb.append(",").append(roasStr);
            }
            sb.append("\n");
        });
    }

    public String buildWeeklyReportSystemPrompt() {
        return """
               당신은 10년 경력의 시니어 퍼포먼스 마케팅 애널리스트입니다.
               마케터들이 가장 어려워하는 것은 "이번 주 광고가 왜 잘됐고, 왜 안됐는지"를 설명하는 것입니다.
               수치를 나열하는 것이 아니라, 데이터 뒤의 맥락과 인과관계를 파악해서
               마케터가 다음 주에 바로 실행할 수 있는 인사이트를 제공하는 것이 당신의 역할입니다.

               ## 분석 프로세스 (반드시 이 순서로 사고하세요)

               1단계 — 데이터 검증: 결측일, 전주 데이터가 없는 신규 캠페인, 예산 조기 소진일,
                       모수가 극단적으로 작은 구간을 먼저 식별하고 해석에서 분리합니다.
               2단계 — 유의미한 변화 탐지: 전주 대비 변화 중 노이즈가 아닌 것만 골라냅니다.
               3단계 — 원인 가설 수립: 각 변화에 대해 데이터로 뒷받침되는 가설을 세웁니다.
               4단계 — 우선순위화: 비즈니스 임팩트(지출액 × 변화폭)가 큰 순서로 highlights를 정렬합니다.

               ## 유의미한 변화 판단 기준

               - 전주 대비 변화율 ±10% 미만은 정상 변동 범위로 간주하고 highlight로 다루지 않습니다.
               - 주간 클릭 수 100건 미만인 캠페인의 CVR 변동, 노출 1,000건 미만인 캠페인의 CTR 변동은
                 통계적 노이즈일 가능성이 높으므로 단정적으로 해석하지 않습니다.
                 언급할 경우 반드시 "모수가 작아 변동성이 큰 구간"임을 명시합니다.
               - 전환 수가 한 자릿수인 경우 CVR·ROAS 변화보다 절대 전환 수 관점으로 서술합니다.

               ## 원인 추정 규칙 (가장 중요)

               - 원인은 반드시 입력 데이터에서 확인 가능한 근거와 함께 제시합니다.
                 근거 없는 원인(경쟁사 동향, 시장 트렌드, 알고리즘 변경 등 데이터에 없는 외부 요인)을
                 사실처럼 단정하지 마세요. 외부 요인은 "확인이 필요한 가설"로만 제시할 수 있습니다.
               - [운영 메모]에 프로모션, 소재 교체, 타겟팅 변경 등의 기록이 있으면
                 해당 시점의 지표 변화와 연결하는 것을 최우선 가설로 검토합니다.
               - 가능한 원인을 2~3개 제시하되, 각 원인이 "데이터 근거"인지 "추정"인지 구분합니다.
               - 확실하지 않은 추정은 "~으로 추정됩니다", "~가능성이 높습니다"로 표현합니다.

               ## 분석 시 고려할 맥락 요소

               - 요일 패턴: 주중/주말의 CTR·CVR 차이는 사용자 행동 패턴 차이에서 비롯됩니다.
                 전주 같은 요일과 비교(WoW same-day)를 우선하고, 단순 일간 비교로 오판하지 마세요.
               - 예산 소진율: 예산이 조기 소진된 날은 노출·클릭이 하루 전체를 대표하지 못하므로
                 성과가 과소평가될 수 있습니다. UsageRate 100%인 날은 별도로 표시하고 해석하세요.
               - 캠페인 라이프사이클: 시작 2주 이내 캠페인은 매체 학습 단계이므로
                 성과 하락을 문제로 단정하지 않습니다.
               - 업종 맥락: 입력된 [업종]에 따라 정상 패턴의 기준이 다릅니다.
                 · 이커머스/패션/식품 등 B2C: 주말·저녁 시간대 CVR 상승은 정상 패턴으로 간주
                 · B2B/SaaS: 주말 트래픽·전환 하락은 정상이므로 highlight로 다루지 않음
                 · 여행/레저: 시즌성(연휴, 방학) 영향을 원인 가설에 우선 반영
                 업종이 "미입력"이면 업종 기반 추정을 하지 않고 데이터 패턴만으로 해석합니다.
               - 플랫폼 특성: 네이버·카카오는 검색 의도 기반(하방 퍼널), 구글은 Discovery/검색 혼합,
                 메타는 피드 노출 기반(상방 퍼널)이므로 CTR·CVR의 기대 수준 자체가 다릅니다.
                 플랫폼 간 절대값 비교가 아닌, 각 플랫폼의 전주 대비 변화로 평가하세요.
               - 지표 간 상관관계:
                 · CTR↑ + CVR↓ → 랜딩페이지 또는 소재-랜딩 불일치 가능성
                 · 노출↓ + CTR↑ → 타겟팅 정밀화 또는 입찰 축소로 인한 고품질 노출 집중 가능성
                 · Spend↑ + ROAS↓ → 한계 효율 체감(예산 증액 대비 전환 미비례) 가능성
                 · 노출↑ + CTR↓ → 오디언스 확장으로 인한 관련성 희석 가능성

               ## 엣지 케이스 처리

               - 전주 데이터가 없는 캠페인: 비교 분석 대신 "신규 캠페인 초기 성과" 관점으로만 서술
               - 특정 일자 데이터 결측: 결측 사실을 명시하고 해당 일자를 제외한 비교 수행
               - 전환 0건 캠페인: ROAS·CVR 계산 불가를 명시하고, 지출 대비 클릭 효율로만 평가
               - 전체 데이터가 비어 있거나 형식이 잘못된 경우: 분석을 지어내지 말고
                 weekSummary에 데이터 문제를 명시하고 highlights를 빈 배열로 반환

               ## 출력 규칙

               - 반드시 유효한 JSON만 출력합니다. 마크다운 코드블록(```), 인사말, 설명 문장을
                 JSON 앞뒤에 절대 붙이지 마세요. 응답의 첫 글자는 { 이고 마지막 글자는 } 입니다.
               - highlights는 최대 5개, POSITIVE 최대 3개 / NEGATIVE 최대 2개.
                 비즈니스 임팩트가 큰 순서로 정렬합니다. 유의미한 변화가 적으면 적게 반환해도 됩니다.
               - platformInsights는 이번 주 지출이 발생한 플랫폼만 포함합니다.
               - 모든 수치는 입력 데이터에서 계산 가능한 값만 사용하고, 인용 시 반올림 기준을 통일합니다
                 (비율은 소수 둘째 자리, 금액은 원 단위).
               - 전문 용어는 첫 등장 시 괄호로 간략 설명을 병기합니다.

               ## 출력 JSON 스키마

               {
                 "weekSummary": "이번 주 전체를 한 줄로 요약. 이메일 제목으로 쓸 수 있도록 40자 이내, 핵심 수치 1개 포함",
                 "highlights": [
                   {
                     "type": "POSITIVE | NEGATIVE",
                     "title": "성과 포인트 제목",
                     "what": "무슨 일이 일어났는지. 수치와 전주 대비 변화 포함, 1~2문장",
                     "why": "추정 원인 2~3개. 가장 유력한 원인을 먼저 쓰고, 각각 데이터 근거인지 추정인지 구분",
                     "evidence": "why를 뒷받침하는 입력 데이터의 구체적 수치",
                     "confidence": "HIGH | MEDIUM | LOW",
                     "action": "다음 주 실행 가능한 구체적 액션 1문장"
                   }
                 ],
                 "platformInsights": [
                   {
                     "platform": "NAVER | GOOGLE | META | KAKAO",
                     "oneLineSummary": "이 플랫폼의 이번 주 한 줄 평가",
                     "keyObservation": "눈에 띄는 패턴 또는 이상 징후 (근거 수치 포함)"
                   }
                 ],
                 "dataCaveats": ["해석에 주의가 필요한 데이터 이슈 목록. 없으면 빈 배열"],
                 "nextWeekFocus": {
                   "focus": "다음 주 가장 집중해야 할 1가지",
                   "reason": "그 이유 (임팩트 관점)",
                   "expectedOutcome": "실행 시 기대되는 변화"
                 }
               }
               """;
    }

    public String buildWeeklyReportUserPrompt(
            String orgName,
            String industry,
            String operationNotes,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate prevStartDate,
            LocalDate prevEndDate,
            List<MetricFact> thisWeekMetrics,
            List<MetricFact> prevWeekMetrics) {

        StringBuilder sb = new StringBuilder();

        sb.append("## 분석 요청\n\n");
        sb.append(String.format("**분석 기간**: %s ~ %s (이번 주 7일)\n", startDate, endDate));
        sb.append(String.format("**비교 기간**: %s ~ %s (전주 7일)\n", prevStartDate, prevEndDate));
        sb.append(String.format("**조직**: %s\n", orgName));
        sb.append(String.format("**업종**: %s\n", industry != null && !industry.isBlank() ? industry : "미입력"));
        sb.append("\n---\n\n");

        sb.append("### [운영 메모]\n");
        sb.append(operationNotes != null && !operationNotes.isBlank() ? operationNotes : "없음");
        sb.append("\n\n---\n\n");

        sb.append("### [주간 집계 요약 — 반드시 이 수치를 기준으로 분석하세요]\n");
        appendWeeklyAggregatedSummary(sb, thisWeekMetrics, prevWeekMetrics);
        sb.append("\n---\n\n");

        sb.append("### [캠페인 현황]\n");
        sb.append("CampaignId,CampaignName,Platform,Goal,BudgetType,StartDate,Budget,TotalSpend,UsageRate(%),Status\n");
        appendWeeklyCampaignCsv(sb, thisWeekMetrics);
        sb.append("\n---\n\n");

        sb.append("### [광고 구조 참조 (CampaignId→GroupId→ContentId 매핑)]\n");
        sb.append("CampaignId,GroupId,GroupName,TargetingInfo,BidAmount,ContentId,ContentName,ContentType,CTA\n");
        appendAdStructureReference(sb, thisWeekMetrics);
        sb.append("\n---\n\n");

        sb.append("### [이번 주 일별 성과 데이터]\n");
        sb.append("Date,DayOfWeek,Platform,CampaignId,Impressions,Clicks,Conversions,Spend,Revenue,CTR(%),CVR(%),ROAS\n");
        appendWeeklyMetricCsv(sb, thisWeekMetrics);
        sb.append("\n---\n\n");

        sb.append("### [전주 일별 성과 데이터 (비교용)]\n");
        sb.append("Date,DayOfWeek,Platform,CampaignId,Impressions,Clicks,Conversions,Spend,Revenue,CTR(%),CVR(%),ROAS\n");
        appendWeeklyMetricCsv(sb, prevWeekMetrics);
        sb.append("\n---\n\n");

        sb.append("위 데이터를 바탕으로 이번 주 광고 성과를 분석해주세요.\n");
        sb.append("마케터가 \"이번 주 광고는 왜 이렇게 됐지?\"라는 질문에 바로 답할 수 있도록,\n");
        sb.append("수치 나열이 아닌 원인과 맥락 중심으로 분석해주세요.\n");
        sb.append("운영 메모에 기록된 변경 사항이 있다면 지표 변화와의 연관성을 최우선으로 검토해주세요.");

        return sb.toString();
    }

    public WeeklyReportResponse.KpiOverview calculateKpiOverview(
            List<MetricFact> thisWeek, List<MetricFact> prevWeek) {

        BigDecimal thisSpend = sumDecimal(thisWeek, m -> m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO);
        BigDecimal prevSpend = sumDecimal(prevWeek, m -> m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO);
        long thisConversions = sum(thisWeek, m -> m.getConversions() != null ? m.getConversions() : 0L);
        long prevConversions = sum(prevWeek, m -> m.getConversions() != null ? m.getConversions() : 0L);
        BigDecimal thisRevenue = sumDecimal(thisWeek, m -> m.getRevenue() != null ? m.getRevenue() : BigDecimal.ZERO);
        BigDecimal prevRevenue = sumDecimal(prevWeek, m -> m.getRevenue() != null ? m.getRevenue() : BigDecimal.ZERO);

        double thisRoas = thisSpend.compareTo(BigDecimal.ZERO) > 0
                ? thisRevenue.divide(thisSpend, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
        double prevRoas = prevSpend.compareTo(BigDecimal.ZERO) > 0
                ? prevRevenue.divide(prevSpend, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;

        return WeeklyReportResponse.KpiOverview.builder()
                .totalSpend(buildKpiMetric(thisSpend.doubleValue(), prevSpend.doubleValue()))
                .totalConversions(buildKpiMetric((double) thisConversions, (double) prevConversions))
                .blendedRoas(buildKpiMetric(thisRoas, prevRoas))
                .build();
    }

    private WeeklyReportResponse.KpiMetric buildKpiMetric(double thisVal, double prevVal) {
        double changeRate = prevVal > 0
                ? Math.round((thisVal - prevVal) / prevVal * 10000.0) / 100.0
                : (thisVal > 0 ? 100.0 : 0.0);
        return WeeklyReportResponse.KpiMetric.builder()
                .thisWeek(thisVal)
                .prevWeek(prevVal)
                .changeRate(changeRate)
                .build();
    }

    private void appendWeeklyAggregatedSummary(StringBuilder sb,
                                                List<MetricFact> thisWeek,
                                                List<MetricFact> prevWeek) {
        long thisClicks = sum(thisWeek, m -> m.getClicks() != null ? m.getClicks() : 0L);
        long thisImpressions = sum(thisWeek, m -> m.getImpressions() != null ? m.getImpressions() : 0L);
        long thisConversions = sum(thisWeek, m -> m.getConversions() != null ? m.getConversions() : 0L);
        BigDecimal thisSpend = sumDecimal(thisWeek, m -> m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO);
        BigDecimal thisRevenue = sumDecimal(thisWeek, m -> m.getRevenue() != null ? m.getRevenue() : BigDecimal.ZERO);

        long prevClicks = sum(prevWeek, m -> m.getClicks() != null ? m.getClicks() : 0L);
        long prevImpressions = sum(prevWeek, m -> m.getImpressions() != null ? m.getImpressions() : 0L);
        long prevConversions = sum(prevWeek, m -> m.getConversions() != null ? m.getConversions() : 0L);
        BigDecimal prevSpend = sumDecimal(prevWeek, m -> m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO);
        BigDecimal prevRevenue = sumDecimal(prevWeek, m -> m.getRevenue() != null ? m.getRevenue() : BigDecimal.ZERO);

        double thisCtr = thisImpressions > 0 ? (double) thisClicks / thisImpressions * 100 : 0;
        double prevCtr = prevImpressions > 0 ? (double) prevClicks / prevImpressions * 100 : 0;
        double thisCvr = thisClicks > 0 ? (double) thisConversions / thisClicks * 100 : 0;
        double prevCvr = prevClicks > 0 ? (double) prevConversions / prevClicks * 100 : 0;
        double thisRoas = thisSpend.compareTo(BigDecimal.ZERO) > 0
                ? thisRevenue.divide(thisSpend, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
        double prevRoas = prevSpend.compareTo(BigDecimal.ZERO) > 0
                ? prevRevenue.divide(prevSpend, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;

        sb.append("지표,이번주,전주,변화량,변화율(%)\n");
        appendSummaryRow(sb, "노출수", thisImpressions, prevImpressions);
        appendSummaryRow(sb, "클릭수", thisClicks, prevClicks);
        appendSummaryRow(sb, "전환수", thisConversions, prevConversions);
        appendSummaryDecimalRow(sb, "지출(원)", thisSpend, prevSpend);
        appendSummaryDecimalRow(sb, "매출(원)", thisRevenue, prevRevenue);
        appendSummaryDoubleRow(sb, "CTR(%)", thisCtr, prevCtr);
        appendSummaryDoubleRow(sb, "CVR(%)", thisCvr, prevCvr);
        appendSummaryDoubleRow(sb, "ROAS(%)", thisRoas, prevRoas);
    }

    private void appendSummaryRow(StringBuilder sb, String label, long thisVal, long prevVal) {
        long diff = thisVal - prevVal;
        String rate = prevVal > 0
                ? String.format("%.1f", (double) diff / prevVal * 100)
                : (thisVal > 0 ? "신규" : "N/A");
        sb.append(String.format("%s,%d,%d,%+d,%s\n", label, thisVal, prevVal, diff, rate));
    }

    private void appendSummaryDecimalRow(StringBuilder sb, String label, BigDecimal thisVal, BigDecimal prevVal) {
        BigDecimal diff = thisVal.subtract(prevVal);
        String rate = prevVal.compareTo(BigDecimal.ZERO) > 0
                ? String.format("%.1f", diff.doubleValue() / prevVal.doubleValue() * 100)
                : (thisVal.compareTo(BigDecimal.ZERO) > 0 ? "신규" : "N/A");
        sb.append(String.format("%s,%s,%s,%+.0f,%s\n", label,
                thisVal.toPlainString(), prevVal.toPlainString(), diff.doubleValue(), rate));
    }

    private void appendSummaryDoubleRow(StringBuilder sb, String label, double thisVal, double prevVal) {
        double diff = thisVal - prevVal;
        String rate = prevVal > 0
                ? String.format("%.1f", diff / prevVal * 100)
                : (thisVal > 0 ? "신규" : "N/A");
        sb.append(String.format("%s,%.2f,%.2f,%+.2f,%s\n", label, thisVal, prevVal, diff, rate));
    }

    private long sum(List<MetricFact> metrics, java.util.function.Function<MetricFact, Long> extractor) {
        return metrics.stream().mapToLong(extractor::apply).sum();
    }

    private BigDecimal sumDecimal(List<MetricFact> metrics, java.util.function.Function<MetricFact, BigDecimal> extractor) {
        return metrics.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void appendWeeklyCampaignCsv(StringBuilder sb, List<MetricFact> metrics) {
        Map<Long, WeeklyCampaignAccumulator> campaignMap = new LinkedHashMap<>();
        for (MetricFact m : metrics) {
            AdCampaign camp = m.getAdContent().getAdGroup().getAdCampaign();
            Long campId = camp.getId();
            BigDecimal spend = m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO;
            campaignMap.computeIfAbsent(campId, id -> new WeeklyCampaignAccumulator(camp)).addSpend(spend);
        }

        for (WeeklyCampaignAccumulator acc : campaignMap.values()) {
            String usageRate = acc.budget == null || acc.budget == 0
                    ? "N/A"
                    : String.format("%.1f", acc.totalSpend.doubleValue() / acc.budget * 100);
            sb.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    acc.campaignId,
                    acc.name != null ? acc.name : "-",
                    acc.platform != null ? acc.platform.name() : "-",
                    acc.goal != null ? acc.goal.name() : "-",
                    acc.budgetType != null ? acc.budgetType.name() : "-",
                    acc.startDate != null ? acc.startDate : "-",
                    acc.budget != null ? acc.budget : "N/A",
                    acc.totalSpend.toPlainString(),
                    usageRate,
                    acc.status != null ? acc.status.name() : "-"));
        }
    }

    private void appendAdStructureReference(StringBuilder sb, List<MetricFact> metrics) {
        record AdStructureKey(Long campaignId, Long groupId, Long contentId) {}
        Map<AdStructureKey, String[]> seen = new LinkedHashMap<>();

        for (MetricFact m : metrics) {
            AdCampaign camp = m.getAdContent().getAdGroup().getAdCampaign();
            AdGroup group = m.getAdContent().getAdGroup();
            AdContent content = m.getAdContent();

            AdStructureKey key = new AdStructureKey(camp.getId(), group.getId(), content.getId());
            seen.computeIfAbsent(key, k -> new String[]{
                    camp.getId().toString(),
                    group.getId().toString(),
                    group.getName() != null ? group.getName() : "-",
                    group.getTargetingInfo() != null ? group.getTargetingInfo().replace(",", ";") : "-",
                    group.getBidAmount() != null ? group.getBidAmount().toString() : "-",
                    content.getId().toString(),
                    content.getName() != null ? content.getName() : "-",
                    content.getType() != null ? content.getType() : "-",
                    content.getCta() != null ? content.getCta() : "-"
            });
        }

        for (String[] row : seen.values()) {
            sb.append(String.join(",", row)).append("\n");
        }
    }

    private void appendWeeklyMetricCsv(StringBuilder sb, List<MetricFact> metrics) {
        record MetricKey(String date, String dayOfWeek, String platform, String campaignId) {}

        Map<MetricKey, WeeklyMetricAccumulator> map = new LinkedHashMap<>();
        for (MetricFact m : metrics) {
            if (m.getTimeBucket() == null) continue;
            LocalDate date = m.getTimeBucket().toLocalDate();
            String dayOfWeek = toDayOfWeekKorean(date.getDayOfWeek());
            String platform = m.getProvider() != null ? m.getProvider().name() : "UNKN";
            String campId = m.getAdContent().getAdGroup().getAdCampaign().getId().toString();
            MetricKey key = new MetricKey(date.toString(), dayOfWeek, platform, campId);
            map.computeIfAbsent(key, k -> new WeeklyMetricAccumulator()).add(m);
        }

        for (Map.Entry<MetricKey, WeeklyMetricAccumulator> entry : map.entrySet()) {
            MetricKey key = entry.getKey();
            WeeklyMetricAccumulator acc = entry.getValue();

            double ctr = acc.impressions > 0
                    ? (double) acc.clicks / acc.impressions * 100 : 0;
            double cvr = acc.clicks > 0
                    ? (double) acc.conversions / acc.clicks * 100 : 0;
            double roas = acc.spend.compareTo(BigDecimal.ZERO) > 0
                    ? acc.revenue.divide(acc.spend, 4, RoundingMode.HALF_UP)
                              .multiply(BigDecimal.valueOf(100))
                              .doubleValue()
                    : 0;

            sb.append(String.format("%s,%s,%s,%s,%d,%d,%d,%s,%s,%.2f,%.2f,%.2f\n",
                    key.date(), key.dayOfWeek(), key.platform(), key.campaignId(),
                    acc.impressions, acc.clicks, acc.conversions,
                    acc.spend.toPlainString(), acc.revenue.toPlainString(),
                    ctr, cvr, roas));
        }
    }

    private String toDayOfWeekKorean(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
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

    private static class WeeklyCampaignAccumulator {
        final Long campaignId;
        final String name;
        final Provider platform;
        final Goal goal;
        final BudgetType budgetType;
        final LocalDate startDate;
        final Long budget;
        final Status status;
        BigDecimal totalSpend = BigDecimal.ZERO;

        WeeklyCampaignAccumulator(AdCampaign camp) {
            this.campaignId = camp.getId();
            this.name = camp.getName();
            this.platform = camp.getProvider();
            this.goal = camp.getGoal();
            this.budgetType = camp.getBudgetType();
            this.startDate = camp.getStartDate();
            this.budget = camp.getBudget();
            this.status = camp.getStatus();
        }

        void addSpend(BigDecimal spend) {
            this.totalSpend = this.totalSpend.add(spend);
        }
    }

    private static class WeeklyMetricAccumulator {
        long impressions = 0;
        long clicks = 0;
        long conversions = 0;
        BigDecimal spend = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;

        void add(MetricFact m) {
            impressions += m.getImpressions() != null ? m.getImpressions() : 0L;
            clicks += m.getClicks() != null ? m.getClicks() : 0L;
            conversions += m.getConversions() != null ? m.getConversions() : 0L;
            spend = spend.add(m.getSpend() != null ? m.getSpend() : BigDecimal.ZERO);
            revenue = revenue.add(m.getRevenue() != null ? m.getRevenue() : BigDecimal.ZERO);
        }
    }
}