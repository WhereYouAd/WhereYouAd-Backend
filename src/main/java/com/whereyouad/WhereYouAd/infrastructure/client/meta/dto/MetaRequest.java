package com.whereyouad.WhereYouAd.infrastructure.client.meta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MetaRequest {

    public record MetaManualSyncRequest(
            @NotBlank(message = "시작 날짜는 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식이 올바르지 않습니다. (예: 2024-03-01)")
            String startDate,
            @NotBlank(message = "종료 날짜는 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식이 올바르지 않습니다. (예: 2024-03-31)")
            String endDate
    ) {}
}
