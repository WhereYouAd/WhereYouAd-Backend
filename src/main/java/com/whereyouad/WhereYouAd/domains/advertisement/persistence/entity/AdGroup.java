package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "ad_group", uniqueConstraints = {
        // 플랫폼 내 같은 adGroup 중복 저장 방지
        @UniqueConstraint(
                name = "uk_ad_campaign_external_group", // 인덱스 이름
                columnNames = {"ad_campaign_id", "external_group_id"} // 복합 키 지정
        )
})
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

    @Column(name = "budget")
    private Long budget;

    @Column(name = "bid_amount")
    private Long bidAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_campaign_id")
    private AdCampaign adCampaign;

    // UPSERT 용 메서드
    public void update(String name, Status status, String targetingInfo) {
        this.name = name;
        this.status = status;
        if (targetingInfo != null) {
            this.targetingInfo = targetingInfo;
        }
    }

//    public void updateBudget(Long budget, Long bidAmount) {
//        if (budget != null) this.budget = budget;
//        if (bidAmount != null) this.bidAmount = bidAmount;
//    }

    public void updateBudget(Long budget, Long bidAmount) {
        if (this.adCampaign.getProvider() == Provider.NAVER) {
            if (budget != null) this.budget = budget;
            if (bidAmount != null) this.bidAmount = bidAmount;
        }

        if (this.adCampaign.getProvider() == Provider.META) {
            this.budget = budget;
            if (bidAmount != null) this.bidAmount = bidAmount;
        }
    }
}
