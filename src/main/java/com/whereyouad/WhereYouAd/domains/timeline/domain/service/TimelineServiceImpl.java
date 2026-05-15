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
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.domain.util.TimelineUtil;
import com.whereyouad.WhereYouAd.domains.timeline.exception.TimelineException;
import com.whereyouad.WhereYouAd.domains.timeline.exception.code.TimelineErrorCode;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

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
        // 조직 검증
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 조직 멤버 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_FORBIDDEN));

        // 날짜 검증(시작일이 종료일보다 늦은 경우)
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
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_FORBIDDEN));

        // ADMIN 권한이 없는 경우
        if (member.getRole() != OrgRole.ADMIN) {
            throw new TimelineException(TimelineErrorCode.TIMELINE_FORBIDDEN);
        }

        // 삭제
        timelineRepository.delete(timeline);
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
}
