package com.whereyouad.WhereYouAd.domains.ai.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class AIRequest {
    // 분석할 기간과 플랫폼
    public record AnalysisRequest(
            @NotNull(message = "시작 날짜는 필수입니다.")
            @PastOrPresent(message = "시작 날짜는 미래일 수 없습니다.")
            LocalDate startDate,

            @NotNull(message = "종료 날짜는 필수입니다.")
            @PastOrPresent(message = "종료 날짜는 미래일 수 없습니다.")
            LocalDate endDate,

            @NotBlank(message = "플랫폼(provider)은 필수입니다.")
            String provider
    ) {}

}