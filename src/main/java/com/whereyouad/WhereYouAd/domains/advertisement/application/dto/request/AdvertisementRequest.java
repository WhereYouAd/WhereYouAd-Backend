package com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

    public record MetaBudgetUpdateRequest(
            @Positive(message = "일일 예산은 0보다 커야 합니다.")
            Long dailyBudget,
            @Positive(message = "총 예산은 0보다 커야 합니다.")
            Long lifetimeBudget
    ) {
        @JsonIgnore
        @AssertTrue(message = "일일 예산과 총 예산 중 정확히 하나만 입력해야 합니다.")
        public boolean isExactlyOne() {
            return (dailyBudget == null) ^ (lifetimeBudget == null);
        }

        @JsonIgnore
        public Long amount() {
            return lifetimeBudget != null ? lifetimeBudget : dailyBudget;
        }

        @JsonIgnore
        public BudgetType budgetType() {
            return lifetimeBudget != null ? BudgetType.TOTAL : BudgetType.DAILY;
        }
    }

    public record GoogleBudgetUpdateRequest(
            @Positive(message = "일일 예산은 0보다 커야 합니다.")
            Long dailyBudget,
            @Positive(message = "총 예산은 0보다 커야 합니다.")
            Long lifetimeBudget
    ) {
        @JsonIgnore
        @AssertTrue(message = "일일 예산과 총 예산 중 정확히 하나만 입력해야 합니다.")
        public boolean isExactlyOne() {
            return (dailyBudget == null) ^ (lifetimeBudget == null);
        }

        @JsonIgnore
        public Long amount() {
            return lifetimeBudget != null ? lifetimeBudget : dailyBudget;
        }

        @JsonIgnore
        public BudgetType budgetType() {
            return lifetimeBudget != null ? BudgetType.TOTAL : BudgetType.DAILY;
        }
    }

}
