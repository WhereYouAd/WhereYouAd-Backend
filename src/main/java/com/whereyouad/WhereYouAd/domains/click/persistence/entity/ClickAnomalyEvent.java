package com.whereyouad.WhereYouAd.domains.click.persistence.entity;

import com.whereyouad.WhereYouAd.domains.click.domain.constant.BaselineSource;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.SurgeDetectionBasis;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클릭 급증 감지 이벤트 이력 (감사 로그 + 추후 대시보드 "이상 이력" 조회용).
 */
@Entity
@Getter
@Table(name = "click_anomaly_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_anomaly_ad_window",
                        columnNames = {"ad_content_id", "window_start"}
                )
        },
        indexes = {
                @Index(name = "idx_anomaly_org_created", columnList = "org_id, created_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClickAnomalyEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "click_anomaly_event_id")
    private Long id;

    // 급증이 감지된 광고 ID
    @Column(name = "ad_content_id", nullable = false)
    private Long adContentId;

    // 광고 소유 조직 ID (알림 대상). active set 멤버 "{adId}:{orgId}"에서 추출
    @Column(name = "org_id", nullable = false)
    private Long orgId;

    // 감지된 5분 윈도우의 시작 시각 (KST). (ad_content_id, window_start) 유니크로 중복 실행 멱등 처리
    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    // 해당 윈도우의 실제 클릭수
    @Column(name = "window_clicks", nullable = false)
    private int windowClicks;

    // 판정 당시 baseline 평균. baseline 전무(COLD_START)면 null
    @Column(name = "baseline_mean")
    private Double baselineMean;

    // 판정 당시 baseline 표준편차. baseline 전무면 null
    @Column(name = "baseline_std")
    private Double baselineStd;

    // (클릭수 - 평균) / σ_eff. 사후 임계값 튜닝 근거로 보존
    @Column(name = "z_score")
    private Double zScore;

    // 클릭수 / 평균 배수. 알림 문구("평소 대비 N배")와 튜닝 근거로 보존
    @Column(name = "multiplier_ratio")
    private Double multiplierRatio;

    // 판정 근거: MULTIPLIER(배수) / Z_SCORE / BOTH / COLD_START(절대 기준)
    @Enumerated(EnumType.STRING)
    @Column(name = "detection_basis", nullable = false, length = 20)
    private SurgeDetectionBasis detectionBasis;

    // baseline 출처: EMA(장기 통계) / ROLLING(직전 1시간) / NONE(없음)
    @Enumerated(EnumType.STRING)
    @Column(name = "baseline_source", nullable = false, length = 10)
    private BaselineSource baselineSource;

    // 실제 알림 발송 여부. streak 미달·쿨다운·드라이런이면 감지돼도 false
    @Column(name = "notified", nullable = false)
    private boolean notified;

    public void markNotified() {
        this.notified = true;
    }
}
