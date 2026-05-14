package com.whereyouad.WhereYouAd.domains.timeline.presentation.docs;

import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface TimelineControllerDocs {

    /**
     * Create a timeline for the specified organization.
     *
     * @param userId the authenticated user's ID
     * @param orgId  the organization ID in which to create the timeline
     * @param dto    timeline details including name, start/end dates, performance metric criteria, and comparison period
     * @return the created timeline's data wrapped in a DataResponse
     */
    @Operation(
            summary = "타임라인 생성 API",
            description = "타임라인 이름, 기간, 성과 기준 지표, 비교 기준 기간을 입력받아 타임라인을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400_1", description = "종료일이 시작일보다 앞선 경우"),
            @ApiResponse(responseCode = "404_1", description = "조직을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<TimelineResponse.CreateResponseDTO>> createTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody TimelineRequest.TimelineCreateDto dto
    );

    /**
     * Deletes the specified timeline from the given organization; only an authenticated member with the organization's ADMIN role may perform this action.
     *
     * @param userId     the authenticated user's id extracted from the security principal
     * @param orgId      the organization id containing the timeline
     * @param timelineId the id of the timeline to delete
     * @return           a ResponseEntity wrapping a DataResponse with no payload indicating successful deletion
     */
    @Operation(
            summary = "타임라인 삭제 API",
            description = "조직의 ADMIN 역할을 가진 멤버만 삭제 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403_1", description = "삭제 권한 없음 (ADMIN이 아닌 경우)"),
            @ApiResponse(responseCode = "404_1", description = "타임라인을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<Void>> deleteTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId
    );
}
