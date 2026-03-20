package com.whereyouad.WhereYouAd.domains.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserInfoModifyRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,
        String oldPassword,
        String newPassword
) {
}
