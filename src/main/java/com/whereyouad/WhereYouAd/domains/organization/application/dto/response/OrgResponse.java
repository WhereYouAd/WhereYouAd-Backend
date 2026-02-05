package com.whereyouad.WhereYouAd.domains.organization.application.dto.response;

import java.time.LocalDateTime;

public class OrgResponse {

    public record Create(
            Long orgId,
            LocalDateTime createdAt
    ) {}

    public record Read (
        //TODO
    ) {}

    public record Update (
            Long orgId,
            String name,
            String description,
            String logoUrl,
            LocalDateTime updatedAt
    ) {}

    //Soft Delete 복구 시 응답값
    public record Delete (
            Long orgId,
            String message
    ) {}

}
