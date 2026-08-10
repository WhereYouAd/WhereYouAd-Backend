package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.domain.config.ClickSurgeProperties;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.BaselineSource;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.SurgeDetectionBasis;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickSurgeCalculator.BaselineSnapshot;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickSurgeCalculator.SurgeVerdict;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickBaselineStat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClickSurgeCalculatorTest {

    // 기본값: multiplier 3.0, z 3.5, minWindowClicks 30, coldStart 90, emaAlpha 0.2
    private final ClickSurgeProperties props = new ClickSurgeProperties();

    private BaselineSnapshot ema(double mean, double std) {
        return new BaselineSnapshot(mean, std, BaselineSource.EMA);
    }

    @Test
    @DisplayName("최소 절대 클릭수 미달이면 배수가 아무리 커도 미감지")
    void guardBlocksLowVolume() {
        // 평소 2회 → 20회 = 10배지만 절대량 부족
        SurgeVerdict verdict = ClickSurgeCalculator.judge(20, ema(2, 1), props);
        assertThat(verdict.detected()).isFalse();
    }

    @Test
    @DisplayName("배수 기준만 충족 - MULTIPLIER")
    void multiplierOnly() {
        // 변동폭이 큰 광고: ratio 3.0 충족, z = 80/30 ≈ 2.67 < 3.5
        SurgeVerdict verdict = ClickSurgeCalculator.judge(120, ema(40, 30), props);
        assertThat(verdict.detected()).isTrue();
        assertThat(verdict.basis()).isEqualTo(SurgeDetectionBasis.MULTIPLIER);
    }

    @Test
    @DisplayName("z-score 기준만 충족 - Z_SCORE (변동폭이 매우 일정한 광고)")
    void zScoreOnly() {
        // std 0 → σ_eff = √40 ≈ 6.32, z = 50/6.32 ≈ 7.9 ≥ 3.5, ratio 2.25 < 3
        SurgeVerdict verdict = ClickSurgeCalculator.judge(90, ema(40, 0), props);
        assertThat(verdict.detected()).isTrue();
        assertThat(verdict.basis()).isEqualTo(SurgeDetectionBasis.Z_SCORE);
    }

    @Test
    @DisplayName("배수 + z-score 모두 충족 - BOTH")
    void both() {
        SurgeVerdict verdict = ClickSurgeCalculator.judge(200, ema(40, 5), props);
        assertThat(verdict.detected()).isTrue();
        assertThat(verdict.basis()).isEqualTo(SurgeDetectionBasis.BOTH);
        assertThat(verdict.ratio()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("σ 하한(√μ) 동작 - 저분산 슬롯에서 소폭 상승은 미감지")
    void sigmaFloorSuppresssSmallBump() {
        // std 0이어도 σ_eff = √40 → z = 20/6.32 ≈ 3.16 < 3.5, ratio 1.5 < 3
        SurgeVerdict verdict = ClickSurgeCalculator.judge(60, ema(40, 0), props);
        assertThat(verdict.detected()).isFalse();
    }

    @Test
    @DisplayName("baseline 없음 - cold-start 절대 기준")
    void coldStart() {
        SurgeVerdict over = ClickSurgeCalculator.judge(100, BaselineSnapshot.none(), props);
        assertThat(over.detected()).isTrue();
        assertThat(over.basis()).isEqualTo(SurgeDetectionBasis.COLD_START);

        SurgeVerdict under = ClickSurgeCalculator.judge(50, BaselineSnapshot.none(), props);
        assertThat(under.detected()).isFalse();
    }

    @Test
    @DisplayName("EMA 갱신 - 초기화 후 수식대로 평균/분산 누적")
    void emaUpdate() {
        ClickBaselineStat stat = ClickBaselineStat.init(1L, 1, 15);

        ClickSurgeCalculator.applyEma(stat, 40, 0.2);
        assertThat(stat.getEmaMean()).isEqualTo(40.0);
        assertThat(stat.getEmaVariance()).isEqualTo(0.0);
        assertThat(stat.getSampleCount()).isEqualTo(1);

        // d=20 → mean = 40 + 0.2*20 = 44, var = 0.8*(0 + 0.2*400) = 64
        ClickSurgeCalculator.applyEma(stat, 60, 0.2);
        assertThat(stat.getEmaMean()).isCloseTo(44.0, within(1e-9));
        assertThat(stat.getEmaVariance()).isCloseTo(64.0, within(1e-9));
        assertThat(stat.getSampleCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("롤링 baseline - 전부 0이면 NONE, 값이 있으면 표본 평균/표준편차")
    void rollingBaseline() {
        assertThat(ClickSurgeCalculator.fromRollingWindows(List.of(0L, 0L, 0L)).source())
                .isEqualTo(BaselineSource.NONE);

        BaselineSnapshot rolling = ClickSurgeCalculator.fromRollingWindows(List.of(30L, 50L));
        assertThat(rolling.source()).isEqualTo(BaselineSource.ROLLING);
        assertThat(rolling.mean()).isEqualTo(40.0);
        assertThat(rolling.std()).isCloseTo(10.0, within(1e-9));
    }
}
