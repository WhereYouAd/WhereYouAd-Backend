package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;

import java.util.List;

public interface TimelineService {
    TimelineResponse.CreateResponseDTO createTimeline(Long userId, Long orgId, TimelineRequest.TimelineCreateDto dto);
    TimelineResponse.CreateResponseDTO updateTimeline(Long userId, Long orgId, Long timelineId, TimelineRequest.TimelineCreateDto dto);
    void deleteTimeline(Long userId, Long orgId, Long timelineId);
    List<TimelineResponse.TimelineSummaryDTO> getTimelines(
            Long userId,
            Long orgId,
            TimelineRequest.TimelineListQuery query
    );
    TimelineResponse.TimelineDetailDTO getTimelineDetail(Long userId, Long orgId, Long timelineId);
    void updateTimelineOrder(Long userId, Long orgId, TimelineRequest.TimelineOrderUpdateDto dto);
    void requestTimelineSummary(Long userId, Long orgId, Long timelineId);
}
