package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;

import java.util.List;

public interface TimelineService {
    TimelineResponse.CreateResponseDTO createTimeline(Long userId, Long orgId, TimelineRequest.TimelineCreateDto dto);
    void deleteTimeline(Long userId, Long orgId, Long timelineId);
    List<TimelineResponse.TimelineSummaryDTO> getTimelines(Long userId, Long orgId);
}