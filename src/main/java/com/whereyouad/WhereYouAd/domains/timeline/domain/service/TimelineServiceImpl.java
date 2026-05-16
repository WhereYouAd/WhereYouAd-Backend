package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.application.mapper.TimelineConverter;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.domain.util.TimelineUtil;
import com.whereyouad.WhereYouAd.domains.timeline.exception.TimelineException;
import com.whereyouad.WhereYouAd.domains.timeline.exception.code.TimelineErrorCode;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelineServiceImpl implements TimelineService {

    private final TimelineRepository timelineRepository;
    private final MetricFactRepository metricFactRepository;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final TimelineUtil timelineUtil;

    @Override
    public TimelineResponse.CreateResponseDTO createTimeline(Long userId, Long orgId, TimelineRequest.TimelineCreateDto dto) {
        // 1. 조직 검증
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 날짜 검증(시작일이 종료일보다 늦은 경우)
        if (dto.endDate().isBefore(dto.startDate())) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE);
        }

        // 비교 기준 날짜(지난 주, 지난 달, 지난 년도와 비교)
        ComparisonDateRange comparisonDates = calculateComparisonDates(dto.startDate(), dto.endDate(), dto.comparisonPeriodType());

        // 비교 기간에 성과 데이터가 없으면 타임라인 생성 불가
        boolean hasComparisonData = metricFactRepository.existsByTimeBucketBetweenAndOrg(
                comparisonDates.start().atStartOfDay(),
                comparisonDates.end().atTime(LocalTime.MAX),
                orgId
        );
        if (!hasComparisonData) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NO_COMPARISON_DATA);
        }

        // 입력받은 DTO를 타임라인 엔티티로 변환
        Timeline timeline = TimelineConverter.toTimeline(dto, organization, userId, comparisonDates.start(), comparisonDates.end());

        // PerformanceStatus 계산 및 판별 로직 호출 및 저장
        PerformanceStatus status = timelineUtil.calculatePerformanceStatus(timeline);
        timeline.updatePerformanceStatus(status);

        // 엔티티 저장 및 반환
        return TimelineConverter.toCreateResponse(timelineRepository.save(timeline));
    }

    @Override
    public void deleteTimeline(Long userId, Long orgId, Long timelineId) {

        // 타임라인이 없는 경우
        Timeline timeline = timelineRepository.findById(timelineId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND));

        // 조직에 타임라인 검증에 실패한 경우
        if (!timeline.getOrganization().getId().equals(orgId)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND);
        }

        // 조직 맴버가 아닌 경우
        OrgMember member = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_DELETE_FORBIDDEN));

        // ADMIN 권한이 없는 경우
        if (member.getRole() != OrgRole.ADMIN) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_DELETE_FORBIDDEN);
        }

        // 삭제
        timelineRepository.delete(timeline);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineResponse.TimelineSummaryDTO> getTimelines(Long userId, Long orgId) {
        // 조직이 없는 경우
        orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 해당 조직의 맴버가 아닌 경우
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_READ_FORBIDDEN));

        List<Timeline> timelines = timelineRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return TimelineConverter.toTimelineSummaryList(timelines);
    }

    @Override
    @Transactional(readOnly = true)
    public TimelineResponse.TimelineDetailDTO getTimelineDetail(Long userId, Long orgId, Long timelineId) {

        // 조직 검증
        orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 조직 맴버 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_READ_FORBIDDEN));

        // 타임라인 검증
        Timeline timeline = timelineRepository.findById(timelineId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND));

        // 타임라인 조직 검증
        if (!timeline.getOrganization().getId().equals(orgId)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND);
        }

        // 선택된 지표 리스트 생성
        List<MetricType> metrics = buildMetricList(timeline);

        // 날짜에 해당하는 지표값 불러오기
        List<MetricFact> facts = metricFactRepository.findByOrgAndPeriodAndGrain(
                orgId,
                timeline.getStartDate().atStartOfDay(),
                timeline.getEndDate().plusDays(1).atStartOfDay(),
                Grain.DAILY
        );

        // 일별 데이터 리스트로 변환
        List<TimelineResponse.DailyMetricDTO> dailyTrend = buildDailyTrend(facts, timeline);
        // 플랫폼별 기여도 반환
        List<TimelineResponse.PlatformContributionDTO> platformContributions = buildPlatformContributions(facts, timeline);

        return TimelineConverter.toTimelineDetailDTO(timeline, metrics, dailyTrend, platformContributions);
    }

    // 선택된 지표를 리스트로 변환해주는 메서드
    private List<MetricType> buildMetricList(Timeline timeline) {
        List<MetricType> metrics = new ArrayList<>();
        if (timeline.isUseClick()) metrics.add(MetricType.CLICK);
        if (timeline.isUseConversion()) metrics.add(MetricType.CONVERSION);
        if (timeline.isUseImpression()) metrics.add(MetricType.IMPRESSION);
        if (timeline.isUseRoas()) metrics.add(MetricType.ROAS);
        return metrics;
    }

    // 날짜별 지표 추이 목록을 생성하는 메서드
    private List<TimelineResponse.DailyMetricDTO> buildDailyTrend(List<MetricFact> facts, Timeline timeline) {
        // 날짜별로 MetricFact 그룹핑
        Map<LocalDate, List<MetricFact>> byDate = facts.stream()
                .collect(Collectors.groupingBy(f -> f.getTimeBucket().toLocalDate()));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<MetricFact> dayFacts = entry.getValue();

                    // 활성화된 지표만 집계(비활성 지표는 null 반환)
                    Long clicks = timeline.isUseClick()
                            ? dayFacts.stream().mapToLong(f -> f.getClicks() != null ? f.getClicks() : 0L).sum()
                            : null;
                    Long conversions = timeline.isUseConversion()
                            ? dayFacts.stream().mapToLong(f -> f.getConversions() != null ? f.getConversions() : 0L).sum()
                            : null;
                    Long impressions = timeline.isUseImpression()
                            ? dayFacts.stream().mapToLong(f -> f.getImpressions() != null ? f.getImpressions() : 0L).sum()
                            : null;

                    // ROAS = revenue / spend (spend가 0이면 null 반환)
                    BigDecimal roas = null;
                    if (timeline.isUseRoas()) {
                        BigDecimal totalSpend = dayFacts.stream()
                                .map(f -> f.getSpend() != null ? f.getSpend() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalRevenue = dayFacts.stream()
                                .map(f -> f.getRevenue() != null ? f.getRevenue() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
                            roas = totalRevenue.divide(totalSpend, 2, RoundingMode.HALF_UP);
                        }
                    }

                    return new TimelineResponse.DailyMetricDTO(date, clicks, conversions, impressions, roas);
                })
                .toList();
    }

    // 플랫폼(provider)별 기여율 목록을 생성하는 메서드
    private List<TimelineResponse.PlatformContributionDTO> buildPlatformContributions(List<MetricFact> facts, Timeline timeline) {
        // 플랫폼별로 MetricFact 그룹핑
        Map<String, List<MetricFact>> byProvider = facts.stream()
                .collect(Collectors.groupingBy(f -> f.getProvider().name()));

        // 활성화된 지표별로 전체 합산
        record MetricTotals(long clicks, long conversions, long impressions, BigDecimal spend) {}
        Map<String, MetricTotals> providerTotals = byProvider.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<MetricFact> pf = e.getValue();
                            return new MetricTotals(
                                    pf.stream().mapToLong(f -> f.getClicks() != null ? f.getClicks() : 0L).sum(),
                                    pf.stream().mapToLong(f -> f.getConversions() != null ? f.getConversions() : 0L).sum(),
                                    pf.stream().mapToLong(f -> f.getImpressions() != null ? f.getImpressions() : 0L).sum(),
                                    pf.stream().map(f -> f.getSpend() != null ? f.getSpend() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                            );
                        }
                ));

        // 활성화된 지표별 전체 합계
        long totalClicks = providerTotals.values().stream().mapToLong(MetricTotals::clicks).sum();
        long totalConversions = providerTotals.values().stream().mapToLong(MetricTotals::conversions).sum();
        long totalImpressions = providerTotals.values().stream().mapToLong(MetricTotals::impressions).sum();
        BigDecimal totalSpend = providerTotals.values().stream().map(MetricTotals::spend).reduce(BigDecimal.ZERO, BigDecimal::add);

        return providerTotals.entrySet().stream()
                .map(entry -> {
                    MetricTotals t = entry.getValue();
                    List<Double> rates = new ArrayList<>();

                    // 활성화된 지표별 기여율 계산 (해당 플랫폼 수치 / 전체 수치 * 100)
                    if (timeline.isUseClick() && totalClicks > 0)
                        rates.add(t.clicks() * 100.0 / totalClicks);
                    if (timeline.isUseConversion() && totalConversions > 0)
                        rates.add(t.conversions() * 100.0 / totalConversions);
                    if (timeline.isUseImpression() && totalImpressions > 0)
                        rates.add(t.impressions() * 100.0 / totalImpressions);
                    if (timeline.isUseRoas() && totalSpend.compareTo(BigDecimal.ZERO) > 0)
                        rates.add(t.spend().multiply(BigDecimal.valueOf(100)).divide(totalSpend, 2, RoundingMode.HALF_UP).doubleValue());

                    // 기여율 = 활성화된 지표별 비율의 평균
                    double avgRate = rates.isEmpty() ? 0.0
                            : rates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                    return new TimelineResponse.PlatformContributionDTO(entry.getKey(), Math.round(avgRate * 100.0) / 100.0);
                })
                // 기여율 0 제외 후 내림차순 정렬
                .filter(dto -> dto.contributionRate() > 0)
                .sorted(Comparator.comparingDouble(TimelineResponse.PlatformContributionDTO::contributionRate).reversed())
                .toList();
    }

    // 비교 날짜 내부 DTO
    private record ComparisonDateRange(LocalDate start, LocalDate end) {}

    // enum -> 날짜 메서드
    private ComparisonDateRange calculateComparisonDates(LocalDate startDate, LocalDate endDate, ComparisonPeriodType type) {
        LocalDate comparisonEnd = startDate.minusDays(1);
        return switch (type) {
            case LAST_WEEK -> new ComparisonDateRange(comparisonEnd.minusDays(6),   comparisonEnd);
            case LAST_MONTH -> new ComparisonDateRange(comparisonEnd.minusDays(29),  comparisonEnd);
            case LAST_YEAR -> new ComparisonDateRange(startDate.minusYears(1), endDate.minusYears(1));
        };
    }
}
