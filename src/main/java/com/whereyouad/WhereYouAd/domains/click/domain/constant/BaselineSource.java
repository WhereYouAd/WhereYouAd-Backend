package com.whereyouad.WhereYouAd.domains.click.domain.constant;

public enum BaselineSource {
    EMA,      // 요일/시간대별 EMA baseline
    ROLLING,  // warm-up 미달로 직전 1시간 롤링 평균 사용
    NONE      // baseline 없음 (cold-start 절대 기준만 적용)
}
