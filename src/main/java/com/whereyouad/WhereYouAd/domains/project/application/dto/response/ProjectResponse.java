package com.whereyouad.WhereYouAd.domains.project.application.dto.response;

public class ProjectResponse {

    public record CreatedResponse(
            Long projectId,
            String message
    ){}
}
