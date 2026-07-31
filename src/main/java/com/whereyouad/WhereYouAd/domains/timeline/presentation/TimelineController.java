package com.whereyouad.WhereYouAd.domains.timeline.presentation;

import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.domain.service.TimelineService;
import com.whereyouad.WhereYouAd.domains.timeline.presentation.docs.TimelineControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/org/{orgId}/timeline")
public class TimelineController implements TimelineControllerDocs {

    private final TimelineService timelineService;

    @GetMapping
    public ResponseEntity<DataResponse<List<TimelineResponse.TimelineSummaryDTO>>> getTimelines(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "LATEST") String sort
    ) {
        TimelineRequest.TimelineListQuery query = new TimelineRequest.TimelineListQuery(status, sort);
        return ResponseEntity.ok(DataResponse.from(timelineService.getTimelines(userId, orgId, query)));
    }

    @GetMapping("/{timelineId}")
    public ResponseEntity<DataResponse<TimelineResponse.TimelineDetailDTO>> getTimelineDetail(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId
    ) {
        return ResponseEntity.ok(DataResponse.from(timelineService.getTimelineDetail(userId, orgId, timelineId)));
    }

    @PostMapping
    public ResponseEntity<DataResponse<TimelineResponse.CreateResponseDTO>> createTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody TimelineRequest.TimelineCreateDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DataResponse.created(timelineService.createTimeline(userId, orgId, dto)));
    }

    @PutMapping("/{timelineId}")
    public ResponseEntity<DataResponse<TimelineResponse.CreateResponseDTO>> updateTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId,
            @Valid @RequestBody TimelineRequest.TimelineCreateDto dto
    ) {
        return ResponseEntity.ok(DataResponse.from(timelineService.updateTimeline(userId, orgId, timelineId, dto)));
    }

    @DeleteMapping("/{timelineId}")
    public ResponseEntity<DataResponse<Void>> deleteTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId
    ) {
        timelineService.deleteTimeline(userId, orgId, timelineId);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @PostMapping("/{timelineId}/summary")
    public ResponseEntity<DataResponse<Void>> requestTimelineSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId
    ) {
        timelineService.requestTimelineSummary(userId, orgId, timelineId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DataResponse.ok());
    }
}
