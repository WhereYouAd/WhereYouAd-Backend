package com.whereyouad.WhereYouAd.global.utils;

import com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 공통으로 사용하는 예산 계산 로직
@Component
public class BudgetCalculator {

    // 캠페인 리스트에서 총 예산 합산 (null budget은 0 처리)
    public long calculateTotalBudget(List<ProjectQueryDto.CampaignSummary> campaignSummaries) {
        return campaignSummaries.stream()
                .mapToLong(summary -> summary.budget() != null ? summary.budget() : 0L)
                .sum();
    }

    // 잔액 계산 (총 예산 - 총 지출)
    public long calculateRemainingBudget(long totalBudget, long totalSpend) {
        return totalBudget - totalSpend;
    }

    // 캠페인 리스트 기반 예산 소진율 계산 (BigDecimal 지출)
    public double calculateBudgetUsageRate(List<ProjectQueryDto.CampaignSummary> campaignSummaries,
            BigDecimal totalSpend) {
        long totalBudget = calculateTotalBudget(campaignSummaries);
        return calculateUsageRate(totalBudget, totalSpend);
    }

    // 예산 소진율 계산 (지출 / 예산 * 100), 소수점 첫째 자리까지 버림
    public double calculateUsageRate(long totalBudget, BigDecimal totalSpend) {
        if (totalBudget <= 0 || totalSpend == null) {
            return 0.0;
        }
        return totalSpend
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBudget), 1, RoundingMode.DOWN)
                .doubleValue();
    }
}
