package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.application.mapper.TimelineConverter;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.TimelineSortType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.util.TimelineUtil;
import com.whereyouad.WhereYouAd.domains.timeline.exception.TimelineException;
import com.whereyouad.WhereYouAd.domains.timeline.exception.code.TimelineErrorCode;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.BudgetHistory;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.BudgetHistoryRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelineServiceImpl implements TimelineService {

    private final TimelineRepository timelineRepository;
    private final MetricFactRepository metricFactRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final TimelineUtil timelineUtil;
    private final TimelineAsyncService timelineAsyncService;

    @Override
    public TimelineResponse.CreateResponseDTO createTimeline(Long userId, Long orgId, TimelineRequest.TimelineCreateDto dto) {
        // 조직 검증
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 조직 멤버 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_READ_FORBIDDEN));

        // 날짜 검증(시작일이 종료일보다 늦은 경우)
        if (dto.endDate().isBefore(dto.startDate())) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE);
        }

        // 비교 기준 날짜(지난 주, 지난 달, 지난 년도와 비교)
        ComparisonDateRange comparisonDates = calculateComparisonDates(dto.startDate(), dto.endDate(), dto.comparisonPeriodType());

        // 비교 기간 성과 합계 조회 (Projection 사용)
        MetricSumProjection pastFacts = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                orgId,
                comparisonDates.start().atStartOfDay(),
                comparisonDates.end().plusDays(1).atStartOfDay(),
                OrgStatus.ACTIVE
        );

        // 비교 기간에 성과 데이터가 없거나 모두 0이면 타임라인 생성 불가
        if (isProjectionEmpty(pastFacts)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NO_COMPARISON_DATA);
        }

        // 현재 기간 성과 합계 조회 (Projection 사용)
        MetricSumProjection currentFacts = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                orgId,
                dto.startDate().atStartOfDay(),
                dto.endDate().plusDays(1).atStartOfDay(),
                OrgStatus.ACTIVE,
                Status.ON_GOING
        );

        // 현재 기간에 성과 데이터가 없거나 모두 0이면 타임라인 생성 불가
        if (isProjectionEmpty(currentFacts)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NO_CURRENT_DATA);
        }

        // 입력받은 DTO를 타임라인 엔티티로 변환
        Timeline timeline = TimelineConverter.toTimeline(dto, organization, userId, comparisonDates.start(), comparisonDates.end());
        orgRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));
        int nextDisplayOrder = timelineRepository.findMaxDisplayOrderByOrganizationId(orgId) + 1;
        timeline.updateDisplayOrder(nextDisplayOrder);

        // 성과 상태 - PerformanceStatus 계산 및 판별 로직 호출 및 저장 (초안)
        PerformanceStatus status = timelineUtil.calculatePerformanceStatus(timeline, currentFacts, pastFacts);
        timeline.updatePerformanceStatus(status);

        // 엔티티 저장 및 반환
        return TimelineConverter.toCreateResponse(timelineRepository.save(timeline));
    }

    @Override
    public TimelineResponse.CreateResponseDTO updateTimeline(Long userId, Long orgId, Long timelineId, TimelineRequest.TimelineCreateDto dto) {
        // 1. 동일 조직의 타임라인 쓰기 작업을 직렬화하도록 조직 행 잠금
        orgRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 타임라인 검증
        Timeline timeline = timelineRepository.findById(timelineId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND));

        // 3. 타임라인 조직 소속 검증
        if (!timeline.getOrganization().getId().equals(orgId)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND);
        }

        // 4. 조직 멤버 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_UPDATE_FORBIDDEN));

        // 5. 날짜 검증
        if (dto.endDate().isBefore(dto.startDate())) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE);
        }

        // 6. 비교 기준 날짜 재계산
        ComparisonDateRange comparisonDates = calculateComparisonDates(dto.startDate(), dto.endDate(), dto.comparisonPeriodType());

        // 7. 비교 기간 성과 합계 조회 (Projection 사용)
        MetricSumProjection pastFacts = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                orgId,
                comparisonDates.start().atStartOfDay(),
                comparisonDates.end().plusDays(1).atStartOfDay(),
                OrgStatus.ACTIVE
        );

        // 비교 기간에 성과 데이터가 없거나 모두 0이면 예외 처리
        if (isProjectionEmpty(pastFacts)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NO_COMPARISON_DATA);
        }

        // 현재 기간 성과 합계 조회
        MetricSumProjection currentFacts = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                orgId,
                dto.startDate().atStartOfDay(),
                dto.endDate().plusDays(1).atStartOfDay(),
                OrgStatus.ACTIVE,
                Status.ON_GOING
        );

        // 현재 기간에 성과 데이터가 없거나 모두 0이면 수정 불가
        if (isProjectionEmpty(currentFacts)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NO_CURRENT_DATA);
        }

        // 8. 성과 리스트 -> boolean 플래그 변환
        boolean useClick = dto.metrics().contains(MetricType.CLICK);
        boolean useConversion = dto.metrics().contains(MetricType.CONVERSION);
        boolean useImpression = dto.metrics().contains(MetricType.IMPRESSION);
        boolean useRoas = dto.metrics().contains(MetricType.ROAS);

        // 9. 엔티티 업데이트
        timeline.update(dto.name(), dto.startDate(), dto.endDate(),
                useClick, useConversion, useImpression, useRoas,
                comparisonDates.start(), comparisonDates.end(),
                dto.comparisonPeriodType());
        // AI 요약도 초기화
        timeline.updateSummary(null);

        // 10. PerformanceStatus 재계산
        PerformanceStatus status = timelineUtil.calculatePerformanceStatus(timeline, currentFacts, pastFacts);
        timeline.updatePerformanceStatus(status);

        // 11. 변환 후 반환
        return TimelineConverter.toCreateResponse(timeline);
    }

    @Override
    public void deleteTimeline(Long userId, Long orgId, Long timelineId) {

        // 동일 조직의 타임라인 쓰기 작업을 직렬화하도록 조직 행 잠금
        orgRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 타임라인이 없는 경우
        Timeline timeline = timelineRepository.findById(timelineId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND));

        // 조직에 타임라인 검증에 실패한 경우
        if (!timeline.getOrganization().getId().equals(orgId)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND);
        }

        // 조직 맴버가 아닌 경우
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_DELETE_FORBIDDEN));

        // 삭제
        timelineRepository.delete(timeline);
    }

    @Override
    public void updateTimelineOrder(Long userId, Long orgId, TimelineRequest.TimelineOrderUpdateDto dto) {
        // 동일 조직에서 생성 또는 순서 변경이 동시에 수행되지 않도록 조직 행 잠금
        orgRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 조직에 소속된 회원만 공용 타임라인 순서 변경 가능
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_UPDATE_FORBIDDEN));

        // 순서 변경 중 다른 트랜잭션이 대상 타임라인을 변경하지 못하도록 전체 행 잠금
        List<Timeline> timelines = timelineRepository.findAllByOrganizationIdForUpdate(orgId);
        List<Long> requestedIds = dto.timelineIds();

        // 요청 ID의 중복 여부와 조직에 실제로 존재하는 전체 타임라인 ID인지 검증
        Set<Long> uniqueRequestedIds = new HashSet<>(requestedIds);
        Set<Long> organizationTimelineIds = timelines.stream()
                .map(Timeline::getId)
                .collect(Collectors.toSet());

        boolean invalidOrder = requestedIds.size() != timelines.size()
                || uniqueRequestedIds.size() != requestedIds.size()
                || !uniqueRequestedIds.equals(organizationTimelineIds);
        if (invalidOrder) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_INVALID_DISPLAY_ORDER);
        }

        // ID로 엔티티를 빠르게 찾을 수 있도록 변환한 뒤 요청 배열의 순서대로 재배치
        Map<Long, Timeline> timelineById = timelines.stream()
                .collect(Collectors.toMap(Timeline::getId, timeline -> timeline));
        for (int index = 0; index < requestedIds.size(); index++) {
            // displayOrder 내림차순 조회 시 요청의 첫 번째 타임라인이 최상단에 위치
            int displayOrder = requestedIds.size() - index - 1;
            timelineById.get(requestedIds.get(index)).updateDisplayOrder(displayOrder);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineResponse.TimelineSummaryDTO> getTimelines(
            Long userId,
            Long orgId,
            TimelineRequest.TimelineListQuery query
    ) {
        PerformanceStatus status = parsePerformanceStatus(query.status());
        TimelineSortType sortType = parseTimelineSortType(query.sort());

        // 조직이 없는 경우
        orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 해당 조직의 맴버가 아닌 경우
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_READ_FORBIDDEN));

        Sort sort = buildTimelineSort(sortType);
        List<Timeline> timelines = status == null
                ? timelineRepository.findAllByOrganizationId(orgId, sort)
                : timelineRepository.findAllByOrganizationIdAndPerformanceStatus(orgId, status, sort);
        return TimelineConverter.toTimelineSummaryList(timelines);
    }

    private PerformanceStatus parsePerformanceStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return PerformanceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_INVALID_STATUS_FILTER);
        }
    }

    private TimelineSortType parseTimelineSortType(String sort) {
        if (sort == null || sort.isBlank()) {
            return TimelineSortType.DISPLAY_ORDER;
        }

        try {
            return TimelineSortType.valueOf(sort);
        } catch (IllegalArgumentException e) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_INVALID_SORT_TYPE);
        }
    }

    private Sort buildTimelineSort(TimelineSortType sortType) {
        return switch (sortType) {
            case DISPLAY_ORDER -> Sort.by(Sort.Direction.DESC, "displayOrder")
                    .and(Sort.by(Sort.Direction.DESC, "endDate", "id"));
            case LATEST -> Sort.by(Sort.Direction.DESC, "endDate", "id");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "endDate", "id");
        };
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

        // 타임라인 기간 내 예산 변경 이력 조회
        List<BudgetHistory> histories = budgetHistoryRepository.findByOrgAndPeriod(
                orgId,
                timeline.getStartDate().atStartOfDay(),
                timeline.getEndDate().plusDays(1).atStartOfDay()
        );
        List<TimelineResponse.BudgetHistoryItem> budgetHistories = TimelineConverter.toBudgetHistoryItems(histories);

        return TimelineConverter.toTimelineDetailDTO(timeline, metrics, dailyTrend, platformContributions, budgetHistories);
    }

    @Override
    public void requestTimelineSummary(Long userId, Long orgId, Long timelineId) {
        orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_READ_FORBIDDEN));

        Timeline timeline = timelineRepository.findById(timelineId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND));

        // 해당 조직의 타임라인이 아닌 경우
        if (!timeline.getOrganization().getId().equals(orgId)) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND);
        }

        // 생성 이후 MetricFact 변동 or 광고 상태 변경을 대비한 재검증
        boolean hasData = metricFactRepository.existsByTimeBucketBetweenAndOrg(
                timeline.getStartDate().atStartOfDay(),
                timeline.getEndDate().plusDays(1).atStartOfDay(),
                orgId
        );
        if (!hasData) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_NO_METRIC_DATA);
        }

        // 트랜잭션 훅 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            // 트랜잭션이 성공적으로 커밋된 시점 이후에 호출
            public void afterCommit() {
                timelineAsyncService.summarizeAsync(timelineId, orgId);
            }
        });
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

        return timeline.getStartDate().datesUntil(timeline.getEndDate().plusDays(1))
                .map(date -> {
                    List<MetricFact> dayFacts = byDate.getOrDefault(date, List.of());

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
        return switch (type) {
            case LAST_WEEK -> {
                LocalDate lastWeekStart = startDate.with(DayOfWeek.MONDAY).minusWeeks(1);
                yield new ComparisonDateRange(lastWeekStart, lastWeekStart.plusDays(6));
            }
            case LAST_MONTH -> {
                LocalDate firstDayOfLastMonth = startDate.minusMonths(1).withDayOfMonth(1);
                LocalDate lastDayOfLastMonth  = startDate.withDayOfMonth(1).minusDays(1);
                yield new ComparisonDateRange(firstDayOfLastMonth, lastDayOfLastMonth);
            }
            case LAST_YEAR -> new ComparisonDateRange(startDate.minusYears(1), endDate.minusYears(1));
        };
    }

    // Projection 결과가 비어있는지(또는 모든 수치가 0인지) 확인하는 헬퍼 메서드
    private boolean isProjectionEmpty(MetricSumProjection projection) {
        if (projection == null) return true;
        return (projection.getTotalImpressions() == null || projection.getTotalImpressions() == 0L) &&
               (projection.getTotalClicks() == null || projection.getTotalClicks() == 0L) &&
               (projection.getTotalConversions() == null || projection.getTotalConversions() == 0L) &&
               (projection.getTotalSpend() == null || projection.getTotalSpend().compareTo(BigDecimal.ZERO) == 0) &&
               (projection.getTotalRevenue() == null || projection.getTotalRevenue().compareTo(BigDecimal.ZERO) == 0);
    }
}
