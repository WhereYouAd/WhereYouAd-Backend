package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "adGroup")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_group_id")
    private Long id;

    @Column(name = "external_group_id")
    private String externalGroupId;

    private String name;

    private String targetingInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'ON_GOING'")
    private Status status;

    @OneToMany(mappedBy = "adGroup", cascade = CascadeType.ALL)
    private List<AdContent> adContents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_campaign_id")
    private AdCampaign adCampaign;

    // UPSERT 용 메서드
    public void updateFromApi(String name, Status status, String targetingInfo) {
        this.name = name;
        this.status = status;
        this.targetingInfo = targetingInfo;
    }
}
