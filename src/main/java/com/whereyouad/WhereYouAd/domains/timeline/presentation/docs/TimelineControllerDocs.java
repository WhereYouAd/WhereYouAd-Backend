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

public interface TimelineControllerDocs {

    @Operation(
            summary = "타임라인 생성 API",
            description = "타임라인 이름, 기간, 성과 기준 지표, 비교 기준 기간을 입력받아 타임라인을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400_1", description = "종료일이 시작일보다 앞선 경우"),
            @ApiResponse(responseCode = "400_2", description = "비교 기간에 해당하는 성과 데이터가 없는 경우"),
            @ApiResponse(responseCode = "403_1", description = "조직 멤버가 아닌 경우"),
            @ApiResponse(responseCode = "404_1", description = "조직을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<TimelineResponse.CreateResponseDTO>> createTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody TimelineRequest.TimelineCreateDto dto
    );

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
