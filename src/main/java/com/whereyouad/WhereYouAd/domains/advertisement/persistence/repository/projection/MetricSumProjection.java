package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection;

import java.math.BigDecimal;

public interface MetricSumProjection {
    Long getTotalImpressions();
    Long getTotalClicks();
    Long getTotalConversions();
    BigDecimal getTotalSpend();
    BigDecimal getTotalRevenue();
}
