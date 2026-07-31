package com.whereyouad.WhereYouAd.domains.timeline.application.dto.request;

import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class TimelineRequest {

    public record TimelineListQuery(
            String status,
            String sort
    ) {}

    public record TimelineCreateDto(
            @NotBlank(message = "타임라인 이름은 필수입니다.") String name,
            @NotNull(message = "시작일은 필수입니다.") LocalDate startDate,
            @NotNull(message = "종료일은 필수입니다.") LocalDate endDate,
            @NotEmpty(message = "성과 지표를 하나 이상 선택해야 합니다.") List<MetricType> metrics,
            @NotNull(message = "비교 기준 기간은 필수입니다.") ComparisonPeriodType comparisonPeriodType
    ) {}
}
