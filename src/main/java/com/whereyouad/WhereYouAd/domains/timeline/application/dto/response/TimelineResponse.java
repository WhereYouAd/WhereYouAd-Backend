package com.whereyouad.WhereYouAd.domains.timeline.application.dto.response;

import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimelineResponse {

    public record CreateResponseDTO(
            Long timelineId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            List<MetricType> metrics,
            LocalDate comparisonStartDate,
            LocalDate comparisonEndDate,
            PerformanceStatus performanceStatus,
            LocalDateTime createdAt
    ) {}
}
