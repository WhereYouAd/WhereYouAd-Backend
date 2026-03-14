package com.whereyouad.WhereYouAd.domains.project.domain.service;

import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;

public interface ProjectService {

    ProjectResponse.CreatedResponse createProject(Long userId, Long orgId, ProjectRequest.CreateRequest request);

    ProjectResponse.ProjectListResponse getProjects(Long userId, Long orgId);

    ProjectResponse.ProjectInfoResponse getProject(Long userId, Long orgId, Long projectId);

    void updateAllProjectsStatus(Long userId, Long orgId, Status status);
}
