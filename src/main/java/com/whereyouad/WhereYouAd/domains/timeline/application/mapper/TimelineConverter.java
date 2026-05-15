package com.whereyouad.WhereYouAd.domains.timeline.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest.TimelineCreateDto;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TimelineConverter {

    // 타임라인 엔티티로 변환
    public static Timeline toTimeline(
            TimelineCreateDto dto,
            Organization organization,
            Long userId,
            LocalDate comparisonStartDate,
            LocalDate comparisonEndDate
    ) {
        return Timeline.builder()
                .name(dto.name())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .useClick(dto.metrics().contains(MetricType.CLICK))
                .useConversion(dto.metrics().contains(MetricType.CONVERSION))
                .useImpression(dto.metrics().contains(MetricType.IMPRESSION))
                .useRoas(dto.metrics().contains(MetricType.ROAS))
                .comparisonStartDate(comparisonStartDate)
                .comparisonEndDate(comparisonEndDate)
                .createdBy(userId)
                .organization(organization)
                .build();
    }

    // Entity List -> DTO List
    public static List<TimelineResponse.TimelineSummaryDTO> toTimelineSummaryList(List<Timeline> timelines) {
        return timelines.stream()
                .map(TimelineConverter::toTimelineSummaryDTO)
                .toList();
    }

    // entity -> DTO
    public static TimelineResponse.TimelineSummaryDTO toTimelineSummaryDTO(Timeline timeline) {
        return new TimelineResponse.TimelineSummaryDTO(
                timeline.getId(),
                timeline.getName(),
                timeline.getStartDate(),
                timeline.getEndDate(),
                timeline.getPerformanceStatus()
        );
    }

    // entity -> dto
    public static TimelineResponse.CreateResponseDTO toCreateResponse(Timeline timeline) {
        List<MetricType> metrics = new ArrayList<>();
        if (timeline.isUseClick()) metrics.add(MetricType.CLICK);
        if (timeline.isUseConversion()) metrics.add(MetricType.CONVERSION);
        if (timeline.isUseImpression()) metrics.add(MetricType.IMPRESSION);
        if (timeline.isUseRoas()) metrics.add(MetricType.ROAS);

        return new TimelineResponse.CreateResponseDTO(
                timeline.getId(),
                timeline.getName(),
                timeline.getStartDate(),
                timeline.getEndDate(),
                metrics,
                timeline.getComparisonStartDate(),
                timeline.getComparisonEndDate(),
                timeline.getPerformanceStatus(),
                timeline.getCreatedAt()
        );
    }
}
