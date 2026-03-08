package com.whereyouad.WhereYouAd.domains.project.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.util.List;

public class ProjectResponse {

    public record CreatedResponse(
            Long projectId,
            String message
    ){}

    public record ProjectListResponse(
            List<SimpleProjectResponse> projects
    ) {}

    public record SimpleProjectResponse(
            Long projectId,
            String name,
            String description,
            List<Provider> providers,
            Double budgetUsageRate
    ) {}
}
