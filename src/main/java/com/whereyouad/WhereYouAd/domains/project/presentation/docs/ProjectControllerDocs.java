package com.whereyouad.WhereYouAd.domains.project.presentation.docs;

import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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
    public ResponseEntity<DataResponse<ProjectResponse.CreatedResponse>> createProject(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody ProjectRequest.CreateRequest request
    );

    @Operation(
            summary = "캠페인 그룹 정보 조회 API",
            description = "캠페인 그룹을 조회하려는 조직 Id 를 받아 해당 조직에 속한 모든 캠페인 그룹 정보를 조회 합니다. \n\n" +
                    "반환 값에는 각 캠페인 그룹의 Id(projectId),  이름, 설명, 해당 캠페인 그룹에서 진행하는 광고 플랫폼들(providers), 예산 소진 현황(budgetUsageRate) 입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "회원 찾을 수 없음"),
            @ApiResponse(responseCode = "404_1", description = "조직 찾을 수 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직에 회원이 속하지 않음"),
            @ApiResponse(responseCode = "410_1", description = "조직 Soft Deleted")
    })
    public ResponseEntity<DataResponse<ProjectResponse.ProjectListResponse>> getProjects(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );
}
