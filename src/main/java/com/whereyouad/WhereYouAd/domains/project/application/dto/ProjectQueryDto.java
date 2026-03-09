package com.whereyouad.WhereYouAd.domains.project.application.dto;

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
}
