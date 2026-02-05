package com.whereyouad.WhereYouAd.domains.organization.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public class OrgRequest {

    public record Create (
            @NotBlank(message = "조직 이름은 필수입니다.")
            String name,
            String description,
            String logoUrl
    ) {}

    public record Read (
        //TODO
    ) {}

    public record Update (
            @NotBlank(message = "조직 이름은 필수입니다.")
            String name,
            String description,
            String logoUrl
    ) {}

}
