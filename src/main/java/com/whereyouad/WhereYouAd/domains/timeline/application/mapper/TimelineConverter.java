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

    /**
     * Create a Timeline entity from the provided create DTO and contextual data.
     *
     * @param dto the request DTO containing timeline name, start/end dates and selected metrics
     * @param organization the organization that will own the timeline
     * @param userId id of the user who creates the timeline
     * @param comparisonStartDate start date for the comparison period
     * @param comparisonEndDate end date for the comparison period
     * @return the constructed Timeline entity with metric flags and audit/context fields set
     */
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

    /**
     * Convert a Timeline entity into a CreateResponseDTO containing its selected metrics and metadata.
     *
     * Reconstructs the list of MetricType values from the entity's metric flags and maps identifier, name,
     * date ranges, performance status, and creation timestamp into the response DTO.
     *
     * @param timeline the Timeline entity to convert
     * @return a populated TimelineResponse.CreateResponseDTO representing the given timeline
     */
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
