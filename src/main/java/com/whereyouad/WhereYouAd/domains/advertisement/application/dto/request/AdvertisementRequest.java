package com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AdvertisementRequest {
    public record ManualSyncRequest(
            @NotNull(message = "시작 날짜는 필수입니다.")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,
            @NotNull(message = "종료 날짜는 필수입니다.")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate
    ) {
        @JsonIgnore
        @AssertTrue(message = "시작 날짜는 종료 날짜보다 늦을 수 없습니다.")
        public boolean isValidRange() {
            return startDate != null && endDate != null && !startDate.isAfter(endDate);
        }
    }
}
