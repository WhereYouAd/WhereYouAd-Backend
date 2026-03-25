package com.whereyouad.WhereYouAd.domains.platform.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "campaign_platform")
@Getter
public class CampaignPlatform {

    //AdCampaign - PlatformAccount 간 N:M 매핑을 위한 중간 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_platform_id")
    private Long id;

    // AdCampaign과의 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_campaign_id")
    private AdCampaign adCampaign;

    // PlatformAccount와의 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_account_id")
    private PlatformAccount platformAccount;

}
