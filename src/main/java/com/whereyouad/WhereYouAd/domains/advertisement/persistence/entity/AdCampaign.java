package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Goal;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "ad_campaign", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_platform_account_external_campaign",
                columnNames = {"platform_account_id", "external_campaign_id"} // 플랫폼 계정 ID + 외부 캠페인 ID
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_campaign_id")
    private Long id;

    @Column(name = "external_campaign_id")
    private String externalCampaignId;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'ON_GOING'")
    private Status status;

    private Long budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type")
    private Goal goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @OneToMany(mappedBy = "adCampaign", cascade = CascadeType.ALL)
    private List<AdGroup> adGroups = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    //Organization 의존성
    //API 연동 시 리팩터링 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    // 연관관계 추가 : PlatformAccount 와 1:N 연관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_account_id")
    private PlatformAccount platformAccount;

    public void relateProject(Project project) {
        this.project = project;
    }

    // UPSERT 용 메서드
    public void update(String name, Status status, Long budget, Goal goal,
                              LocalDate startDate, LocalDate endDate, String description) {
        this.name = name;
        this.status = status;
        this.budget = budget;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
        if (description != null) {
            this.description = description;
        }
    }

    public void updateBudget(Long budget) {
        this.budget = budget;
    }
}
