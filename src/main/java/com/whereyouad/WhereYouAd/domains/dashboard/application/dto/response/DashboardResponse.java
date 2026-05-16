package com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;

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

    // 실시간 클릭수 스트림 반환 응답
    public record RealTimeGraphResponse(
            List<ClickResponse.RealtimeClickCount> timeSeriesData, // 최근 N분간의 클릭수 배열 (차트 X, Y축 데이터)
            String mode,  // 현재 데이터 모드 ("real" 또는 "dummy")
            Boolean hasSuspect,  // 이상 징후 발생 여부 (빨간 점 트리거)
            SuspectDetail suspectDetail  // 이상 징후 상세 정보 (툴팁 내용)
    ) {}

    //이상 클릭 징후 상세
    public record SuspectDetail(
            String provider,
            String campaignName,
            String adName,
            String message
    ) {}

    // 일자별 및 합계 지표를 담을 레코드
    public record DailyMetricFactResponse(
            LocalDate date,        // 일자 (합계인 경우 null 또는 특정 값)
            Long impressions,      // 노출수
            Long clicks,           // 클릭수
            Long spend,            // 광고비
            Long conversions,      // 전환수
            Long revenue,          // 매출 (ROAS 계산용)
            Double ctr,            // 클릭률 (%)
            Double cpa,            // 전환단가
            Double roas            // 광고수익률 (%)
    ) {}

    // 플랫폼별 대시보드 메트릭 리스트 래퍼 응답
    public record PlatformMetricFactSummaryResponse(
            String providerType,
            LocalDate startDate,
            LocalDate endDate,
            DailyMetricFactResponse total,              // 기간 전체 합계 지표
            List<DailyMetricFactResponse> dailyMetrics  // 일자별 지표 리스트
    ) {}
}
