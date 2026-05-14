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

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/org/{orgId}/timeline")
public class TimelineController implements TimelineControllerDocs {

    private final TimelineService timelineService;

    @PostMapping
    public ResponseEntity<DataResponse<TimelineResponse.CreateResponseDTO>> createTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody TimelineRequest.TimelineCreateDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DataResponse.created(timelineService.createTimeline(userId, orgId, dto)));
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
}
