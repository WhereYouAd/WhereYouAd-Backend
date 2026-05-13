package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
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
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelineServiceImpl implements TimelineService {

    private final TimelineRepository timelineRepository;
    private final OrgRepository orgRepository;
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

        // 입력받은 DTO를 타임라인 엔티티로 변환
        Timeline timeline = TimelineConverter.toTimeline(dto, organization, userId, comparisonDates.start(), comparisonDates.end());

        // PerformanceStatus 계산 및 판별 로직 호출 및 저장
        PerformanceStatus status = timelineUtil.calculatePerformanceStatus(timeline);
        timeline.updatePerformanceStatus(status);

        // 엔티티 저장 및 반환
        return TimelineConverter.toCreateResponse(timelineRepository.save(timeline));
    }

    // 비교 날짜 내부 DTO
    private record ComparisonDateRange(LocalDate start, LocalDate end) {}

    // enum -> 날짜 메서드
    private ComparisonDateRange calculateComparisonDates(LocalDate startDate, LocalDate endDate, ComparisonPeriodType type) {
        return switch (type) {
            case LAST_WEEK -> new ComparisonDateRange(startDate.minusDays(7), endDate.minusDays(7));
            case LAST_MONTH -> new ComparisonDateRange(startDate.minusMonths(1), endDate.minusMonths(1));
            case LAST_YEAR -> new ComparisonDateRange(startDate.minusYears(1), endDate.minusYears(1));
        };
    }
}
