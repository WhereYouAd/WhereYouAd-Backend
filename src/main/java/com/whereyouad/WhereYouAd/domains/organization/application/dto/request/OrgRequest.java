package com.whereyouad.WhereYouAd.domains.organization.application.dto.request;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    public record UpdateRole (
            @Schema(description = "조직 내 역할(ADMIN / MEMBER)", example = "ADMIN", allowableValues = {"ADMIN", "MEMBER"})
            @NotNull(message = "역할은 필수입니다.")
            OrgRole orgRole
    ) {}
}
