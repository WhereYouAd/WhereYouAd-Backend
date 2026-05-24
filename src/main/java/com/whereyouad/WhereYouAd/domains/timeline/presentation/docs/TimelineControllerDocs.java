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

import java.util.List;

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
            summary = "타임라인 목록 조회 API",
            description = "조직의 타임라인 목록을 최신순으로 조회합니다. 각 항목은 제목, 날짜 범위, 성과 상태를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403_2", description = "조직 멤버가 아닌 경우"),
            @ApiResponse(responseCode = "404_1", description = "조직을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<List<TimelineResponse.TimelineSummaryDTO>>> getTimelines(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );

    @Operation(
            summary = "타임라인 상세 조회 API",
            description = "타임라인의 기본 정보, 기간 내 일별 지표 추이, 플랫폼별 기여 비율을 반환합니다. 플랫폼 기여율은 활성화된 지표별 비율의 평균으로 계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403_2", description = "조직 멤버가 아닌 경우"),
            @ApiResponse(responseCode = "404_1", description = "타임라인 또는 조직을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<TimelineResponse.TimelineDetailDTO>> getTimelineDetail(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId
    );

    @Operation(
            summary = "타임라인 수정 API",
            description = "조직의 ADMIN 역할을 가진 멤버만 수정 가능합니다. 날짜 변경 시 비교 기간 및 성과 상태가 재계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400_1", description = "종료일이 시작일보다 앞선 경우"),
            @ApiResponse(responseCode = "400_2", description = "비교 기간에 성과 데이터가 없는 경우"),
            @ApiResponse(responseCode = "403_3", description = "수정 권한 없음 (ADMIN이 아닌 경우)"),
            @ApiResponse(responseCode = "404_1", description = "타임라인 또는 조직을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<TimelineResponse.CreateResponseDTO>> updateTimeline(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId,
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

    @Operation(
            summary = "타임라인 AI 요약 요청 API",
            description = "타임라인 기간의 성과 데이터를 바탕으로 AI 요약문을 생성합니다. 비동기로 처리되며, 완료 후 타임라인 상세 조회(summary 필드)에서 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "요약 요청 수락됨"),
            @ApiResponse(responseCode = "400_3", description = "해당 기간의 광고 데이터가 없는 경우"),
            @ApiResponse(responseCode = "403_2", description = "조직 멤버가 아닌 경우"),
            @ApiResponse(responseCode = "404_1", description = "타임라인 또는 조직을 찾을 수 없는 경우")
    })
    ResponseEntity<DataResponse<Void>> requestTimelineSummary(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long timelineId
    );
}
