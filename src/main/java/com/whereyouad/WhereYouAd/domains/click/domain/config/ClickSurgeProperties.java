package com.whereyouad.WhereYouAd.domains.click.domain.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "click.surge")
public class ClickSurgeProperties {

    private boolean enabled = true;
    private boolean notifyEnabled = true;      // false = 감지·기록만 하고 발송하지 않는 드라이런 모드
    private int windowMinutes = 5;
    private double multiplierThreshold = 3.0;  // baseline 대비 배수 기준
    private double zScoreThreshold = 3.5;
    private int minWindowClicks = 30;          // 윈도우 최소 절대 클릭수 가드 (저볼륨 오탐 방지)
    private int coldStartMinClicks = 90;       // baseline 전무 시 절대치 단독 발동 기준
    private int streakRequired = 2;            // 연속 감지 횟수 (2회 = 10분 지속)
    private long cooldownSeconds = 1800;       // 광고별 알림 쿨다운
    private double emaAlpha = 0.2;
    private int warmupMinSamples = 12;         // EMA 샘플 미만이면 롤링 baseline 사용
    private long activeSetTtlSeconds = 1800;
}
