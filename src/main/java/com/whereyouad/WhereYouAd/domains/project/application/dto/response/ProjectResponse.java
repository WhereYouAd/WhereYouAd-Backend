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
    // adCampaignId: 예산 수정 요청 시 사용할 캠페인 식별자 (내부 PK).
    //   - 구글/메타: 이 값 그대로 PATCH /api/{google|meta}/campaigns/{adCampaignId}/budget 요청에 사용 가능
    //   - 네이버: 예산 수정 API가 내부 PK가 아닌 connectionId + 외부 캠페인ID를 요구하므로 null로 내려감 (대신 naverBudgetTarget 참고)
    // naverBudgetTarget: 네이버 예산 수정(PUT /api/naver/{connectionId}/campaigns/{campaignId}/budget) 요청 조립용, 네이버가 아니면 null
    public record PlatformBudgetSummary(
            Provider provider,
            BudgetType budgetType,
            Long adCampaignId,
            NaverBudgetTarget naverBudgetTarget,
            Long budget,
            Long spend,
            Long remainingBudget,
            Double remainingPercentage
    ) {}

    // 네이버 캠페인 예산 수정 API 요청에 필요한 식별자 묶음
    // connectionId: PUT /api/naver/{connectionId}/campaigns/{campaignId}/budget 의 path 변수
    // campaignId: 위 요청의 {campaignId} path 변수 (네이버 외부 캠페인ID, nccCampaignId)
    public record NaverBudgetTarget(
            Long connectionId,
            String campaignId
    ) {}
}
