package com.whereyouad.WhereYouAd.domains.project.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;

import java.time.LocalDate;
import java.util.List;

public class ProjectResponse {

    public record CreatedResponse(
            Long projectId,
            String message
    ){}

    public record ProjectListResponse(
            List<SimpleProjectResponse> projects
    ) {}

    public record SimpleProjectResponse(
            Long projectId,
            String name,
            Status status,
            String description,
            List<Provider> providers,
            Double budgetUsageRate
    ) {}

    public record ProjectInfoResponse (
        Long projectId,
        String name,
        Status status,
        String description,
        Long budget,
        LocalDate createdAt,
        List<Provider> providers,
        List<PlatformBudgetSummary> platformBudgets
    ) {}

    // 캠페인(프로젝트) 상세 페이지 - 플랫폼별 남은 예산
    // budgetType: 이 플랫폼에서 사용하는 예산 특성 (TOTAL 캠페인이 있으면 TOTAL - 구글/메타, 없으면 DAILY - 네이버)
    public record PlatformBudgetSummary(
            Provider provider,
            BudgetType budgetType,
            Long budget,
            Long spend,
            Long remainingBudget,
            Double remainingPercentage
    ) {}
}
