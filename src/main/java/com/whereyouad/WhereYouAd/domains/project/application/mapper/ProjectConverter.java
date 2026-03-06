package com.whereyouad.WhereYouAd.domains.project.application.mapper;

import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;

public class ProjectConverter {

    //Entity -> DTO
    public static ProjectResponse.CreatedResponse toCreatedResponse(Project project) {
        return new ProjectResponse.CreatedResponse(project.getId(), "캠페인 생성이 완료되었습니다.");
    }
}
