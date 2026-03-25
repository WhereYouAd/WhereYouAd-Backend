package com.whereyouad.WhereYouAd.domains.platform.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Timezone;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "platform_account")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class PlatformAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_account_id")
    private Long id;

    @Column(name = "external_account_id", length = 255, nullable = false)
    private String externalAccountId;

    @Column(name = "account_name", length = 255)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    @ColumnDefault("'KRW'")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "timezone", length = 64)
    @ColumnDefault("'ASIA'")
    private Timezone timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    @ColumnDefault("'ACTIVE'")
    private PlatformStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    //AdCampaign 과의 다대다 매핑을 위한 중간 테이블과 1:N 매핑
    @OneToMany(mappedBy = "platformAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignPlatform> campaignPlatforms = new ArrayList<>();
}
