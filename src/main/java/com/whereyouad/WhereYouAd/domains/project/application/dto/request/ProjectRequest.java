package com.whereyouad.WhereYouAd.domains.project.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ProjectRequest {

    public record CreateRequest(
            @NotBlank(message = "캠페인 그룹명은 필수입니다.")
            String name,
            String description,
            @NotNull(message = "캠페인 Id 리스트는 null 일 수 없습니다.")
            @NotEmpty(message = "최소 1개의 캠페인을 선택해야 합니다.")
            List<Long> campaignIds
    ){}
}
