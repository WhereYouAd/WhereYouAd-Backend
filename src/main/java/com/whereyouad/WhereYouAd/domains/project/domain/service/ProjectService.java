package com.whereyouad.WhereYouAd.domains.project.domain.service;

import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;

public interface ProjectService {

    ProjectResponse.CreatedResponse createProject(Long userId, Long orgId, ProjectRequest.CreateRequest request);

    ProjectResponse.ProjectListResponse getProjects(Long userId, Long orgId);
}
