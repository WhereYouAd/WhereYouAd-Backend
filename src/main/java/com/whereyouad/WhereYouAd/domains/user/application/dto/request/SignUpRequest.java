package com.whereyouad.WhereYouAd.domains.user.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,
        @NotBlank(message = "이름은 필수입니다.")
        String name,
        @NotBlank(message = "전화번호는 필수입니다.")
        @JsonProperty("phone_number")
        String phoneNumber
) {}
