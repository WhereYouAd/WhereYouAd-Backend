package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;

public interface TimelineService {
    TimelineResponse.CreateResponseDTO createTimeline(Long userId, Long orgId, TimelineRequest.TimelineCreateDto dto);
}