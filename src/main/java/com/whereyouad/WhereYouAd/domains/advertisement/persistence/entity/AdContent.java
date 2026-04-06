package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Table(name = "adContent")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_content_id")
    private Long id;

    //광고 개체 외부 Id
    @Column(name = "external_ad_id")
    private String externalAdId;

    private String name;

    private String trackingUrl;

    private String landingUrl;

    private String description;

    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'ON_GOING'")
    private Status status;

    private String cta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_group_id")
    private AdGroup adGroup;

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void updateTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public void updateLandingUrl(String landingUrl) {
        this.landingUrl = landingUrl;
    }

    // UPSERT 용 메서드
    public void updateFromApi(String type, Status status, String description) {
        this.type = type;
        this.status = status;
        this.description = description;
    }
}
