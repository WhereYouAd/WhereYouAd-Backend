package com.whereyouad.WhereYouAd.domains.project.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;

import java.util.List;

public class ProjectConverter {

    //Entity -> DTO
    public static ProjectResponse.CreatedResponse toCreatedResponse(Project project) {
        return new ProjectResponse.CreatedResponse(project.getId(), "캠페인 그룹 생성이 완료되었습니다.");
    }

    public static ProjectResponse.SimpleProjectResponse toSimpleProjectResponse(Project project, List<Provider> providers, Double budgetUsageRate)
    {
        return new ProjectResponse.SimpleProjectResponse(
                project.getId(), project.getName(), project.getDescription(), providers, budgetUsageRate);
    }

    public static ProjectResponse.ProjectListResponse toProjectListResponse(List<ProjectResponse.SimpleProjectResponse> projects) {
        return new ProjectResponse.ProjectListResponse(projects);
    }
}
