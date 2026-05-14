package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;

public interface TimelineService {
    /**
 * Create a new timeline for the specified user within the given organization.
 *
 * @param userId the identifier of the user creating the timeline
 * @param orgId  the identifier of the organization the timeline belongs to
 * @param dto    the timeline creation payload (title, content, visibility, etc.)
 * @return       a DTO representing the created timeline, including its generated identifier and metadata
 */
TimelineResponse.CreateResponseDTO createTimeline(Long userId, Long orgId, TimelineRequest.TimelineCreateDto dto);
    /**
 * Deletes the timeline identified by `timelineId` for the specified user and organization.
 *
 * @param userId     identifier of the user requesting the deletion
 * @param orgId      identifier of the organization that owns the timeline
 * @param timelineId identifier of the timeline to delete
 */
void deleteTimeline(Long userId, Long orgId, Long timelineId);
}