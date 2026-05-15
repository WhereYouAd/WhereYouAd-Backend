package com.whereyouad.WhereYouAd.domains.timeline.domain.util;

import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.springframework.stereotype.Component;

@Component
public class TimelineUtil {
    // TODO: PerformanceStatus 판별 로직 추가
    public PerformanceStatus calculatePerformanceStatus(Timeline timeline) {
        return null;
    }
}
