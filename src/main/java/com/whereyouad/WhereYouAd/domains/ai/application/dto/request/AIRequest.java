package com.whereyouad.WhereYouAd.domains.ai.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class AIRequest {
    // 분석할 기간
    public record PeriodRequest(
            @NotNull(message = "시작 날짜는 필수입니다.")
            @PastOrPresent(message = "시작 날짜는 미래일 수 없습니다.")
            LocalDate startDate,

            @NotNull(message = "종료 날짜는 필수입니다.")
            @PastOrPresent(message = "종료 날짜는 미래일 수 없습니다.")
            LocalDate endDate
    ) {}

}