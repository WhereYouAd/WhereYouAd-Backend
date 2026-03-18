package com.whereyouad.WhereYouAd.domains.click.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClickRequest {

    public record CreateTrackingUrl(
            @Schema(description = "광고 연결 랜딩 URL", example = "https://www.whereyouad.com")
            @NotBlank(message = "Landing URL cannot be empty")
            @Pattern(regexp = "^(https?://).+", message = "랜딩 url은 http:// or https://로 시작해야 합니다.")
            String landingUrl
    ) {}
}
