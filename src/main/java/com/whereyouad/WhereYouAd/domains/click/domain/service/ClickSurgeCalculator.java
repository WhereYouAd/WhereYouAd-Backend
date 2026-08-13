package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.domain.config.ClickSurgeProperties;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.BaselineSource;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.SurgeDetectionBasis;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickBaselineStat;

import java.util.List;

/**
 * 클릭 급증 판정·EMA 갱신 순수 함수 모음 (상태 없음 → 단위 테스트 용이).
 */
public final class ClickSurgeCalculator {

    private ClickSurgeCalculator() {
    }

    public record BaselineSnapshot(double mean, double std, BaselineSource source) {
        public static BaselineSnapshot none() {
            return new BaselineSnapshot(0, 0, BaselineSource.NONE);
        }
    }

    public record SurgeVerdict(boolean detected, SurgeDetectionBasis basis, double zScore, double ratio) {
        public static SurgeVerdict notDetected() {
            return new SurgeVerdict(false, null, 0, 0);
        }
    }

    public static SurgeVerdict judge(long clicks, BaselineSnapshot baseline, ClickSurgeProperties props) {
        // 최소 절대 클릭수 가드 - 저볼륨 광고의 노이즈성 배수 오탐 방지
        if (clicks < props.getMinWindowClicks()) {
            return SurgeVerdict.notDetected();
        }

        // baseline 전무(신규 광고 + 직전 1시간 트래픽도 없음) - 절대 기준 단독 적용
        if (baseline.source() == BaselineSource.NONE) {
            if (clicks >= props.getColdStartMinClicks()) {
                return new SurgeVerdict(true, SurgeDetectionBasis.COLD_START, 0, 0);
            }
            return SurgeVerdict.notDetected();
        }

        // Poisson 하한(√μ) + 절대 하한(1.0) - 저분산 슬롯의 z-score 과민 반응 억제
        double sigmaEff = Math.max(Math.max(baseline.std(), Math.sqrt(Math.max(baseline.mean(), 0))), 1.0);
        double zScore = (clicks - baseline.mean()) / sigmaEff;
        double ratio = clicks / Math.max(baseline.mean(), 1.0);

        boolean byMultiplier = ratio >= props.getMultiplierThreshold();
        boolean byZScore = zScore >= props.getZScoreThreshold();

        if (!byMultiplier && !byZScore) {
            return new SurgeVerdict(false, null, zScore, ratio);
        }

        SurgeDetectionBasis basis = byMultiplier && byZScore ? SurgeDetectionBasis.BOTH
                : byMultiplier ? SurgeDetectionBasis.MULTIPLIER
                : SurgeDetectionBasis.Z_SCORE;
        return new SurgeVerdict(true, basis, zScore, ratio);
    }

    /**
     * EMA 평균·분산 갱신. 급증 판정된 윈도우는 호출하지 않는다 (baseline 오염 방지).
     */
    public static void applyEma(ClickBaselineStat stat, long observed, double alpha) {
        if (stat.getSampleCount() == 0) {
            stat.updateStats(observed, 0, 1);
            return;
        }
        double delta = observed - stat.getEmaMean();
        double newMean = stat.getEmaMean() + alpha * delta;
        double newVariance = (1 - alpha) * (stat.getEmaVariance() + alpha * delta * delta);
        stat.updateStats(newMean, newVariance, stat.getSampleCount() + 1);
    }

    public static BaselineSnapshot fromEma(ClickBaselineStat stat) {
        return new BaselineSnapshot(stat.getEmaMean(), Math.sqrt(Math.max(stat.getEmaVariance(), 0)), BaselineSource.EMA);
    }

    /**
     * warm-up 대체 baseline - 직전 1시간의 5분 윈도우 합계들로 표본 평균/표준편차 계산.
     */
    public static BaselineSnapshot fromRollingWindows(List<Long> windowSums) {
        if (windowSums.isEmpty() || windowSums.stream().allMatch(sum -> sum == 0)) {
            return BaselineSnapshot.none();
        }
        double mean = windowSums.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = windowSums.stream()
                .mapToDouble(sum -> (sum - mean) * (sum - mean))
                .average().orElse(0);
        return new BaselineSnapshot(mean, Math.sqrt(variance), BaselineSource.ROLLING);
    }
}
