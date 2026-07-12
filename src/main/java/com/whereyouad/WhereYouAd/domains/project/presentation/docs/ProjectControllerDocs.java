package com.whereyouad.WhereYouAd.domains.project.presentation.docs;

import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Project API", description = "캠페인 그룹(project) 관련 API")
public interface ProjectControllerDocs {

    @Operation(
            summary = "캠페인 그룹 정보 설정 API",
            description = "캠페인 그룹을 생성하려는 조직의 Id와 조직 이름, 설명, 플랫폼별 캠페인 Id 를 받아 캠페인 그룹을 생성하고, \n\n" +
                    "해당 캠페인 그룹 Id 를 projectId 필드로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "회원 찾을 수 없음"),
            @ApiResponse(responseCode = "404_1", description = "조직 찾을 수 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직에 회원이 속하지 않음"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 의 캠페인 찾을 수 없음"),
            @ApiResponse(responseCode = "410_1", description = "조직 Soft Deleted")
    })
    ResponseEntity<DataResponse<ProjectResponse.CreatedResponse>> createProject(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody ProjectRequest.CreateRequest request
    );

    @Operation(
            summary = "캠페인 그룹 정보 조회 API",
            description = "캠페인 그룹을 조회하려는 조직 Id 를 받아 해당 조직에 속한 모든 캠페인 그룹 정보를 조회 합니다. \n\n" +
                    "반환 값에는 각 캠페인 그룹의 Id(projectId), 이름, 상태, 설명, 해당 캠페인 그룹에서 진행하는 광고 플랫폼들(providers), 예산 소진 현황(budgetUsageRate) 입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "회원 찾을 수 없음"),
            @ApiResponse(responseCode = "404_1", description = "조직 찾을 수 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직에 회원이 속하지 않음"),
            @ApiResponse(responseCode = "410_1", description = "조직 Soft Deleted")
    })
    ResponseEntity<DataResponse<ProjectResponse.ProjectListResponse>> getProjects(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );

    @Operation(
            summary = "특정 캠페인 그룹 조회 API",
            description = "특정 캠페인 그룹을 조회하려는 조직 Id와 캠페인 그룹(project) id를 받아 해당 조직의 특정 캠페인 그룹 정보를 조회합니다. \n\n" +
                    "반환 값에는 각 캠페인 그룹의 Id(projectId), 이름, 상태, 설명, 예산, 등록 날짜, 해당 캠페인 그룹에서 진행하는 광고 플랫폼들(providers)입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "회원 찾을 수 없음"),
            @ApiResponse(responseCode = "404_1", description = "조직 찾을 수 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직에 회원이 속하지 않음"),
            @ApiResponse(responseCode = "410_1", description = "조직 Soft Deleted")
    })
    ResponseEntity<DataResponse<ProjectResponse.ProjectInfoResponse>> getProject(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId, @PathVariable Long projectId
    );

    @Operation(summary = "전체 프로젝트 중단/재개 API", description = "특정 조직에 속한 모든 프로젝트 및 그 하위의 모든 광고 그룹과 콘텐츠 상태를 일괄 중단(PAUSED) 하거나 재개(ON_GOING) 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "회원 찾을 수 없음"),
            @ApiResponse(responseCode = "404_1", description = "조직 찾을 수 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직에 회원이 속하지 않음"),
            @ApiResponse(responseCode = "410_1", description = "조직 Soft Deleted")
    })
    ResponseEntity<DataResponse<Void>> updateAllProjectsStatus(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam Status status);
}
