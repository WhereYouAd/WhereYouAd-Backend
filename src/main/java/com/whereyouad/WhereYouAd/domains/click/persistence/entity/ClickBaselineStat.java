package com.whereyouad.WhereYouAd.domains.click.persistence.entity;

import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 광고별 (요일, 시간대) 슬롯의 5분 윈도우 클릭수 baseline.
 * EMA(지수이동평균)로 O(1) 갱신하므로 과거 데이터 재스캔이 없다.
 */
@Entity
@Getter
@Table(name = "click_baseline_stat", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_baseline_ad_slot",
                columnNames = {"ad_content_id", "weekday", "hour_of_day"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClickBaselineStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "baseline_stat_id")
    private Long id;

    // 대상 광고 ID (연관관계 없이 ID만 보관 - 통계 테이블이라 광고 엔티티 로딩 불필요)
    @Column(name = "ad_content_id", nullable = false)
    private Long adContentId;

    // 요일 슬롯: 1(월) ~ 7(일), DayOfWeek.getValue()
    @Column(name = "weekday", nullable = false)
    private int weekday;

    // 시간대 슬롯: 0 ~ 23. (광고, 요일, 시간대) 조합당 1행
    @Column(name = "hour_of_day", nullable = false)
    private int hourOfDay;

    // 이 슬롯의 5분 윈도우 클릭수 EMA 평균 = "평소 5분에 몇 번 클릭되는가"
    @Column(name = "ema_mean", nullable = false)
    private double emaMean;

    // EMA 분산. 판정 시 √variance로 표준편차를 만들어 z-score 계산에 사용
    @Column(name = "ema_variance", nullable = false)
    private double emaVariance;

    // 이 슬롯에 반영된 윈도우 수. warmup-min-samples(12) 미만이면 EMA 대신 롤링 baseline 사용
    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    public static ClickBaselineStat init(Long adContentId, int weekday, int hourOfDay) {
        return ClickBaselineStat.builder()
                .adContentId(adContentId)
                .weekday(weekday)
                .hourOfDay(hourOfDay)
                .emaMean(0)
                .emaVariance(0)
                .sampleCount(0)
                .build();
    }

    public void updateStats(double emaMean, double emaVariance, int sampleCount) {
        this.emaMean = emaMean;
        this.emaVariance = emaVariance;
        this.sampleCount = sampleCount;
    }
}
