package com.whereyouad.WhereYouAd.domains.timeline.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Table(name = "timeline")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Timeline extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeline_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "use_click", nullable = false)
    @ColumnDefault("false")
    private boolean useClick;

    @Column(name = "use_conversion", nullable = false)
    @ColumnDefault("false")
    private boolean useConversion;

    @Column(name = "use_impression", nullable = false)
    @ColumnDefault("false")
    private boolean useImpression;

    @Column(name = "use_roas", nullable = false)
    @ColumnDefault("false")
    private boolean useRoas;

    @Column(name = "comparison_start_date", nullable = false)
    private LocalDate comparisonStartDate;

    @Column(name = "comparison_end_date", nullable = false)
    private LocalDate comparisonEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_period_type", nullable = false)
    private ComparisonPeriodType comparisonPeriodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status")
    private PerformanceStatus performanceStatus;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    public void updatePerformanceStatus(PerformanceStatus status) {
        this.performanceStatus = status;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }

    public void update(String name,
                       LocalDate startDate,
                       LocalDate endDate,
                       boolean useClick,
                       boolean useConversion,
                       boolean useImpression,
                       boolean useRoas,
                       LocalDate comparisonStartDate,
                       LocalDate comparisonEndDate,
                       ComparisonPeriodType comparisonPeriodType
    ) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.useClick = useClick;
        this.useConversion = useConversion;
        this.useImpression = useImpression;
        this.useRoas = useRoas;
        this.comparisonStartDate = comparisonStartDate;
        this.comparisonEndDate = comparisonEndDate;
        this.comparisonPeriodType = comparisonPeriodType;
    }
}
