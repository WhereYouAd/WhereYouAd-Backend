package com.whereyouad.WhereYouAd.domains.project.application.dto;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.math.BigDecimal;

public class ProjectQueryDto {

    public record CampaignSummary(
            Long projectId,
            Provider provider,
            Long budget
    ){}

    public record SpendSummary(
            Long projectId,
            BigDecimal totalSpend
    ){}

    // 캠페인(프로젝트) 상세 페이지의 플랫폼별 남은 예산 계산용
    public record CampaignBudgetInfo(
            Provider provider,
            BudgetType budgetType,
            Long budget
    ){}
}
