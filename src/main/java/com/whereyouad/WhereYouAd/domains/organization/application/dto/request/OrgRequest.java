package com.whereyouad.WhereYouAd.domains.organization.application.dto.request;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrgRequest {

    public record Create (
            @NotBlank(message = "조직 이름은 필수입니다.")
            String name,
            String description
    ) {}

    public record Read (
        //TODO
    ) {}

    public record Update (
            @NotBlank(message = "조직 이름은 필수입니다.")
            String name,
            String description,
            boolean isImageDeleted
    ) {}

    public record UpdateRole (
            @Schema(description = "조직 내 역할(ADMIN / MEMBER)", example = "ADMIN", allowableValues = {"ADMIN", "MEMBER"})
            @NotNull(message = "역할은 필수입니다.")
            OrgRole orgRole
    ) {}
    public record Invite(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email
    ) {}

    public record ChangeOwner(
            @Schema(description = "워크스페이스 새 소유자로 지정할 사용자의 DB Id", example = "1")
            @NotNull(message = "새 소유자 Id 는 필수입니다.")
            Long newOwnerUserId
    ) {}
}
