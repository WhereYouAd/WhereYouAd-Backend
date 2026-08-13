package com.whereyouad.WhereYouAd.domains.click.domain.constant;

public enum SurgeDetectionBasis {
    MULTIPLIER,   // 배수 기준만 충족
    Z_SCORE,      // z-score 기준만 충족
    BOTH,         // 배수 + z-score 모두 충족
    COLD_START    // baseline 없이 절대 클릭수 기준 충족
}
