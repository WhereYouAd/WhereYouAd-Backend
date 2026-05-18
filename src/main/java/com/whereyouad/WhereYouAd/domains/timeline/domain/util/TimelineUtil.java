package com.whereyouad.WhereYouAd.domains.timeline.domain.util;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TimelineUtil {

    // 성과 상태 판별 로직 초안
    public PerformanceStatus calculatePerformanceStatus(Timeline timeline, MetricSumProjection currentFacts, MetricSumProjection pastFacts) {
        
        // 데이터가 아예 없는 경우 방어 로직 (Projection은 SUM이 없으면 0을 반환하도록 COALESCE 되어있음)
        if (currentFacts == null || pastFacts == null) {
            return PerformanceStatus.ON_TRACK;
        }

        double totalRate = 0.0;
        int activeMetricCount = 0;

        // 1. 클릭수 비교
        if (timeline.isUseClick()) {
            long currentClicks = currentFacts.getTotalClicks() != null ? currentFacts.getTotalClicks() : 0L;
            long pastClicks = pastFacts.getTotalClicks() != null ? pastFacts.getTotalClicks() : 0L;
            
            if (pastClicks > 0) {
                totalRate += (double) currentClicks / pastClicks;
                activeMetricCount++;
            }
        }

        // 2. 전환수 비교
        if (timeline.isUseConversion()) {
            long currentConv = currentFacts.getTotalConversions() != null ? currentFacts.getTotalConversions() : 0L;
            long pastConv = pastFacts.getTotalConversions() != null ? pastFacts.getTotalConversions() : 0L;
            
            if (pastConv > 0) {
                totalRate += (double) currentConv / pastConv;
                activeMetricCount++;
            }
        }

        // 3. 노출수 비교
        if (timeline.isUseImpression()) {
            long currentImp = currentFacts.getTotalImpressions() != null ? currentFacts.getTotalImpressions() : 0L;
            long pastImp = pastFacts.getTotalImpressions() != null ? pastFacts.getTotalImpressions() : 0L;
            
            if (pastImp > 0) {
                totalRate += (double) currentImp / pastImp;
                activeMetricCount++;
            }
        }

        // 4. ROAS 비교
        if (timeline.isUseRoas()) {
            BigDecimal currentSpend = currentFacts.getTotalSpend() != null ? currentFacts.getTotalSpend() : BigDecimal.ZERO;
            BigDecimal currentRev = currentFacts.getTotalRevenue() != null ? currentFacts.getTotalRevenue() : BigDecimal.ZERO;
            BigDecimal currentRoas = currentSpend.compareTo(BigDecimal.ZERO) > 0 ? currentRev.divide(currentSpend, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal pastSpend = pastFacts.getTotalSpend() != null ? pastFacts.getTotalSpend() : BigDecimal.ZERO;
            BigDecimal pastRev = pastFacts.getTotalRevenue() != null ? pastFacts.getTotalRevenue() : BigDecimal.ZERO;
            BigDecimal pastRoas = pastSpend.compareTo(BigDecimal.ZERO) > 0 ? pastRev.divide(pastSpend, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            if (pastRoas.compareTo(BigDecimal.ZERO) > 0) {
                totalRate += currentRoas.divide(pastRoas, 4, RoundingMode.HALF_UP).doubleValue();
                activeMetricCount++;
            }
        }

        // 비교할 유효 지표가 없는 경우
        if (activeMetricCount == 0) {
            return PerformanceStatus.ON_TRACK;
        }

        double avgRate = totalRate / activeMetricCount;

        // 초안: 10% 이상 상승하면 ABOVE_AVG, 10% 이상 하락하면 UNDERPERFORM, 그 외 ON_TRACK
        if (avgRate >= 1.1) {
            return PerformanceStatus.ABOVE_AVG;
        } else if (avgRate <= 0.9) {
            return PerformanceStatus.UNDERPERFORM;
        } else {
            return PerformanceStatus.ON_TRACK;
        }
    }
}
