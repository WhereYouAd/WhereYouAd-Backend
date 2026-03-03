package com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.time.LocalDate;
import java.util.List;

public class DashboardResponse {
    public record BudgetSummaryResponse(
            String providerType,
            Double usagePercentage,
            Long totalBudget,
            Long totalSpend,
            Long remainingBudget
    ) {}

    // Provider별 ROAS 개별 순위
    public record RankingROAS(
            Integer rank, // 순위 (ROAS 내림차순)
            Provider provider, // 광고 플랫폼 (GOOGLE, KAKAO, NAVER)
            Double roas, // ROAS = (revenue / adSpend) * 100
            Integer diffRate, // 이전 동일 기간 대비 ROAS 변화율 (%), null = 비교 데이터 없음
            Long revenue, // 매출 합계 (원)
            Long adSpend // 광고비 합계 (원)
    ) {
    }

    // ROAS 순위 목록 전체 리스트
    public record RankingROASList(
            LocalDate startDate,
            LocalDate endDate,
            List<RankingROAS> rankings) {
    }

    public record AggregatedSummaryResponse(
            Long clicks, //클릭수
            Double clickChangeRate, //클릭수 변화율

            Long impressions, //노출수
            Double impressionChangeRate, //노출수 변화율

            Double conversion, //전환율
            Double cvrChangeRate,  //전환율 변화율

            Double ROAS, //광고비 대비 매출
            Double ROASChangeRate //광고비 대비 매출 변화율
    ) {}

    // 현재 진행 중인 모든 플랫폼의 광고 개수 반환 응답
    public record OngoingPlatformAdCountResponse(
            LocalDate startDate,
            LocalDate endDate,
            Long totalCount,
            List<OngoingPlatformAdCount> providerCount
    ) {}
    // 각 플랫폼에 해당하는 광고 개수
    public record OngoingPlatformAdCount(
            Provider provider,
            Long count
    ) {}
}
