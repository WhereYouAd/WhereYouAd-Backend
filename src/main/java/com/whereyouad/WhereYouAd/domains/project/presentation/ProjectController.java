package com.whereyouad.WhereYouAd.domains.project.presentation;

import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.project.domain.service.ProjectService;
import com.whereyouad.WhereYouAd.domains.project.presentation.docs.ProjectControllerDocs;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/project")
public class ProjectController implements ProjectControllerDocs {

    private final ProjectService projectService;

    @PostMapping("/create/{orgId}")
    public ResponseEntity<DataResponse<ProjectResponse.CreatedResponse>> createProject(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @Valid @RequestBody ProjectRequest.CreateRequest request
    )
    {
        ProjectResponse.CreatedResponse response = projectService.createProject(userId, orgId, request);

        return ResponseEntity.ok(
                DataResponse.created(response)
        );
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<DataResponse<ProjectResponse.ProjectListResponse>> getProjects(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    )
    {
        ProjectResponse.ProjectListResponse response = projectService.getProjects(userId, orgId);

                return ResponseEntity.ok(
                                DataResponse.from(response));
        }

        @PatchMapping("/{orgId}/status")
        public ResponseEntity<DataResponse<Void>> updateAllProjectsStatus(
                        @AuthenticationPrincipal(expression = "userId") Long userId,
                        @PathVariable Long orgId,
                        @RequestParam Status status) {
                projectService.updateAllProjectsStatus(userId, orgId, status);
                return ResponseEntity.ok(DataResponse.ok());
        }

}
