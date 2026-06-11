package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetFieldType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "budget_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BudgetHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_history_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private BudgetFieldType fieldType;

    @Column(name = "previous_value")
    private Long previousValue;

    @Column(name = "new_value")
    private Long newValue;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    // 연관 관계 (nullable로 2개 중에 1개만 연결 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_campaign_id")
    private AdCampaign adCampaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_group_id")
    private AdGroup adGroup;
}
