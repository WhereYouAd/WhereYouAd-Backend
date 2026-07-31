package com.whereyouad.WhereYouAd.domains.ai.persistence.repository.projection;

import com.whereyouad.WhereYouAd.domains.ai.domain.constant.AIStatus;

import java.time.LocalDateTime;

public interface AIReportSummaryProjection {

    Long getReportId();

    String getAccessToken();

    LocalDateTime getPeriodStart();

    LocalDateTime getPeriodEnd();

    AIStatus getStatus();

    String getReportType();

    boolean getShared();

    LocalDateTime getCreatedAt();
}
