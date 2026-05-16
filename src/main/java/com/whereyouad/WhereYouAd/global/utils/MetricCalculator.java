package com.whereyouad.WhereYouAd.global.utils;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MetricCalculator {

    // ROAS = (revenue / spend) * 100
    // spend가 null 또는 0이면 0.0 반환 (Division by Zero 방어)
    public double calculateRoas(BigDecimal revenue, BigDecimal spend) {
        if (spend == null || spend.compareTo(BigDecimal.ZERO) == 0)
            return 0.0;
        if (revenue == null)
            return 0.0;

        return revenue.divide(spend, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // CTR = (clicks / impressions) * 100
    public double calculateCtr(Long clicks, Long impressions) {
        if (impressions == null || impressions == 0)
            return 0.0;
        if (clicks == null)
            return 0.0;

        return BigDecimal.valueOf(clicks)
                .divide(BigDecimal.valueOf(impressions), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // CPA = (spend / conversions)
    public double calculateCpa(BigDecimal spend, Long conversions) {
        if (conversions == null || conversions == 0)
            return 0.0;
        if (spend == null)
            return 0.0;

        return spend.divide(BigDecimal.valueOf(conversions), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // 변화율 = ((current - past) / past) * 100
    // past가 null이거나 0일 경우의 방어 로직 포함
    public Double calculateChangeRate(Number current, Number past) {
        double currentVal = (current != null) ? current.doubleValue() : 0.0;
        double pastVal = (past != null) ? past.doubleValue() : 0.0;

        if (pastVal == 0.0) {
            if (currentVal > 0.0) {
                return 100.0;
            }
            return 0.0;
        }

        double changeRate = ((currentVal - pastVal) / pastVal) * 100.0;

        return BigDecimal.valueOf(changeRate)
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    // 변화율 = ((current - past) / past) * 100 (반올림 정수)
    public Integer computeDiffRate(double currentVal, Double pastVal) {
        if (pastVal == null || pastVal == 0.0)
            return 0;

        double rate = ((currentVal - pastVal) / pastVal) * 100.0;
        return (int) Math.round(rate);
    }

    // 분모가 0일 경우 나눗셈 시행하지 않고 0.0 반환 (BigDecimal 반환)
    public BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0 || numerator == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // 분모가 0일 경우 나눗셈 시행하지 않고 0.0 반환 (double 반환)
    public double safePercent(Number numerator, Number denominator) {
        double n = numerator == null ? 0.0 : numerator.doubleValue();
        double d = denominator == null ? 0.0 : denominator.doubleValue();
        if (d == 0.0) return 0.0;
        return (n / d) * 100.0;
    }

    // 지표 값이 null 일 경우 BigDecimal 의 0 으로 바꿔주는 메서드
    public BigDecimal toZeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
