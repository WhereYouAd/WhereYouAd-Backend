package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "metric_fact")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MetricFact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_fact_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "grain", nullable = false)
    private Grain grain; // 집계 단위(HOURLY, DAILY)

    @Column(name = "time_bucket", nullable = false)
    private LocalDateTime timeBucket; // 집계 단위 시작 시간

//    @Column(name = "dimension_type")
//    private String dimensionType;

    @Column(name = "impressions")
    private Long impressions; // 노출수

    @Column(name = "clicks")
    private Long clicks; // 클릭수

    @Column(name = "conversions")
    private Long conversions; // 전환수

    @Column(name = "spend", precision = 18, scale = 2)
    private BigDecimal spend; // 광고비

    @Column(name = "revenue", precision = 18, scale = 2)
    private BigDecimal revenue; // 매출

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    // 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    //추가 연관관계 -> Advertisement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private AdContent adContent;
}
