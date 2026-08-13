package com.whereyouad.WhereYouAd.domains.notification.application.dto;

import java.util.List;

public record BotClickSummaryData(
        Long orgId,
        String orgName,
        long totalSuspectClicks,
        long distinctIpCount,
        long affectedAdCount,
        List<TopAd> topAds
) {
    public record TopAd(String adName, long clickCount) {
    }
}
