package com.whereyouad.WhereYouAd.domains.click.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.DeviceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_log", indexes = {
        // 봇 클릭 일일 요약 집계용. ddl-auto: update는 기존 테이블에 인덱스를 추가하지 않으므로 운영 DB에는 수동 CREATE INDEX 필요
        @Index(name = "idx_click_log_suspect_clicked", columnList = "is_suspect, clicked_at")
})
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "click_log_id")
    private Long id;

    // 클릭 발생 IP. length 45 = IPv6 최대 길이. 일일 요약의 "유니크 IP 수" 집계에 사용
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // User-Agent에서 추출한 기기 유형 (MOBILE / PC / UNKNOWN)
    @Enumerated(EnumType.STRING)
    @Column(name = "device", length = 512)
    private DeviceType device;

    // 클릭 발생 시각 (KST, ClickWindowKeys.ZONE_ID 기준). 일일 요약의 날짜 경계 필터에 사용
    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    // BotDetector 판별 결과 - true면 봇 의심 클릭. 일일 요약은 이 값이 true인 행만 집계
    @Column(name = "is_suspect", nullable = false)
    private Boolean isSuspect;

    // 클릭된 광고 소재. AdGroup → AdCampaign → Organization으로 조직 집계 시 JOIN 경로
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_content_id", nullable = false)
    private AdContent adContent;
}
