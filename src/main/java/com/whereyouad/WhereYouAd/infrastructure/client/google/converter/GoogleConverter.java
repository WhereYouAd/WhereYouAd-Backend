package com.whereyouad.WhereYouAd.infrastructure.client.google.converter;

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
        if (budget != null && budget.getAmountMicros() != null) {
            budgetAmount = budget.getAmountMicros() / 1_000_000L;
        }

        return AdCampaign.builder()
                .externalCampaignId(campaign != null ? campaign.getId() : null)
                .name(campaign != null ? campaign.getName() : null)
                .provider(Provider.GOOGLE)
                .status(campaign != null ? mapStatus(campaign.getStatus()) : Status.OVER)
                .budget(budgetAmount)
                .startDate(campaign != null ? parseDate(campaign.getStartDate()) : null)
                .endDate(campaign != null ? parseDate(campaign.getEndDate()) : null)
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
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        LocalDate date = parseDate(dateStr);
        return date != null ? date.atStartOfDay() : null;
    }
}
