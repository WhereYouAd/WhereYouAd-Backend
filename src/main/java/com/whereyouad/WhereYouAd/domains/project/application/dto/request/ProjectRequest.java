package com.whereyouad.WhereYouAd.domains.project.application.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ProjectRequest {

    public record CreateRequest(
            @NotBlank(message = "캠페인 그룹명은 필수입니다.")
            String name,
            String description,
            List<Long> campaignIds
    ){}
}
