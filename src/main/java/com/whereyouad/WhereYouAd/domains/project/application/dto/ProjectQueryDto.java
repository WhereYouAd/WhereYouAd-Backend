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
            Long campaignId,
            Provider provider,
            BudgetType budgetType,
            Long budget
    ){}

    // 캠페인(프로젝트) 상세 페이지의 플랫폼별 지출 배치 조회용 (provider 루프 내 개별 쿼리 방지)
    public record ProviderSpend(
            Provider provider,
            BigDecimal totalSpend
    ){}
}
