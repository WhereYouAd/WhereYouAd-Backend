package com.whereyouad.WhereYouAd.infrastructure.client.google.converter;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Goal;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.infrastructure.client.google.dto.GoogleDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Component
public class GoogleConverter {

    public AdCampaign toAdCampaign(GoogleDTO.AdCampaignResult result, PlatformAccount platformAccount) {
        GoogleDTO.AdCampaignNode campaign = result.getCampaign();
        GoogleDTO.AdCampaignBudgetNode budget = result.getCampaignBudget();

        Long budgetAmount = null;
        com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType budgetType = null;
        if (budget != null && budget.getAmountMicros() != null) {
            budgetAmount = budget.getAmountMicros() / 1_000_000L;
            
            if ("DAILY".equalsIgnoreCase(budget.getPeriod())) {
                budgetType = com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType.DAILY;
            } else if (budget.getPeriod() != null) {
                budgetType = com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType.TOTAL;
            } else {
                budgetType = com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType.DAILY; // 기본값
            }
        }

        Goal goal = campaign != null ? mapGoal(campaign.getAdvertisingChannelType()) : null;
        String description = "구글 API 자동 연동 캠페인 (이름 : " + (campaign != null ? campaign.getName() : "알 수 없음") + " 목표: " + (goal != null ? goal.name() : "기타") + ")";

        return AdCampaign.builder()
                .externalCampaignId(campaign != null ? campaign.getId() : null)
                .name(campaign != null ? campaign.getName() : null)
                .provider(Provider.GOOGLE)
                .description(campaign != null ? description : null)
                .status(campaign != null ? mapStatus(campaign.getStatus()) : Status.OVER)
                .budget(budgetAmount)
                .budgetType(budgetType)
                .goal(goal)
                .startDate(campaign != null ? parseDate(campaign.getStartDateTime()) : null)
                .endDate(campaign != null ? parseDate(campaign.getEndDateTime()) : null)
                .organization(platformAccount.getOrganization())
                .platformAccount(platformAccount)
                .build();
    }

    public AdGroup toAdGroup(GoogleDTO.AdGroupResult result, AdCampaign adCampaign) {
        GoogleDTO.AdGroupNode adGroup = result.getAdGroup();

        return AdGroup.builder()
                .externalGroupId(adGroup != null ? adGroup.getId() : null)
                .name(adGroup != null ? adGroup.getName() : null)
                .status(adGroup != null ? mapStatus(adGroup.getStatus()) : Status.OVER)
                .adCampaign(adCampaign)
                .build();
    }

    public AdContent toAdContent(GoogleDTO.AdContentResult result, AdGroup adGroup) {
        GoogleDTO.AdGroupAdNode adGroupAd = result.getAdGroupAd();
        GoogleDTO.AdNode ad = adGroupAd != null ? adGroupAd.getAd() : null;

        String landingUrl = null;
        if (ad != null && ad.getFinalUrls() != null && !ad.getFinalUrls().isEmpty()) {
            landingUrl = ad.getFinalUrls().get(0);
        }

        return AdContent.builder()
                .name(ad != null ? ad.getName() : null)
                .externalAdId(ad != null ? ad.getId() : null)
                .type(ad != null ? ad.getType() : null)
                .trackingUrl(ad != null ? ad.getTrackingUrlTemplate() : null)
                .landingUrl(landingUrl)
                .status(adGroupAd != null ? mapStatus(adGroupAd.getStatus()) : Status.OVER)
                .adGroup(adGroup)
                .build();
    }

    public MetricFact toMetricFact(GoogleDTO.MetricFactResult result, AdCampaign adCampaign, AdContent adContent, Project project, PlatformAccount platformAccount) {
        GoogleDTO.MetricsNode metrics = result.getMetrics();
        GoogleDTO.SegmentsNode segments = result.getSegments();

        Long conversions = 0L;
        if (metrics != null && metrics.getConversions() != null) {
            conversions = metrics.getConversions().longValue();
        }

        BigDecimal spend = BigDecimal.ZERO;
        if (metrics != null && metrics.getCostMicros() != null) {
            spend = BigDecimal.valueOf(metrics.getCostMicros()).divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);
        }

        BigDecimal revenue = BigDecimal.ZERO;
        if (metrics != null && metrics.getConversionsValue() != null) {
            revenue = BigDecimal.valueOf(metrics.getConversionsValue());
        }

        return MetricFact.builder()
                .grain(Grain.DAILY)
                .timeBucket(segments != null ? parseDateTime(segments.getDate()) : null)
                .impressions(metrics != null ? metrics.getImpressions() : 0L)
                .clicks(metrics != null ? metrics.getClicks() : 0L)
                .conversions(conversions)
                .spend(spend)
                .revenue(revenue)
                .provider(Provider.GOOGLE)
                .adCampaign(adCampaign)
                .adContent(adContent)
                .project(project)
                .platformAccount(platformAccount)
                .build();
    }

    private Status mapStatus(String googleStatus) {
        if (googleStatus == null) return Status.OVER;
        switch (googleStatus.toUpperCase()) {
            case "ENABLED":
                return Status.ON_GOING;
            case "PAUSED":
                return Status.PAUSED;
            case "REMOVED":
            default:
                return Status.OVER;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            if (dateStr.length() >= 10) {
                return LocalDate.parse(dateStr.substring(0, 10));
            }
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        LocalDate date = parseDate(dateStr);
        return date != null ? date.atStartOfDay() : null;
    }

    private Goal mapGoal(String advertisingChannelType) {
        if (advertisingChannelType == null) {
            return null;
        }
        switch (advertisingChannelType.toUpperCase()) {
            case "SEARCH":
            case "DISPLAY":
                return Goal.TRAFFIC;
            case "MULTI_CHANNEL":
            case "SHOPPING":
            case "PERFORMANCE_MAX":
                return Goal.POPULAR;
            case "HOTEL":
            case "LOCAL":
            case "APP":
                return Goal.DOWNLOAD;
            default:
                return Goal.POPULAR;
        }
    }
}
