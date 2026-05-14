package com.whereyouad.WhereYouAd.domains.timeline.domain.util;

import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.springframework.stereotype.Component;

@Component
public class TimelineUtil {
    /**
     * Determines the performance status for the given timeline based on its metrics and state.
     *
     * @param timeline the timeline whose performance should be evaluated
     * @return the determined {@code PerformanceStatus} for the timeline, or {@code null} if a status cannot be determined
     */
    public PerformanceStatus calculatePerformanceStatus(Timeline timeline) {
        return null;
    }
}
