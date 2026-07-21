package com.whereyouad.WhereYouAd.domains.notification.application.dto;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;

import java.util.List;

public record WeeklyReportData(
        String orgName,
        List<String> recipients,
        List<MetricFact> thisWeekMetrics,
        List<MetricFact> prevWeekMetrics
) {}
