package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Table(name = "ad_content", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_ad_group_external_ad",
                columnNames = {"ad_group_id", "external_ad_id"} // 광고 그룹 ID + 외부 소재 ID
        )
})
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
    public void update(String name, String type, Status status, String description,
                       String trackingUrl, String landingUrl) {
        this.name = name;
        this.type = type;
        this.status = status;
        if (description != null) {
            this.description = description;
        }
        // 자체 발급한 트래킹 URL은 플랫폼 값으로 덮어쓰지 않는다
        if (trackingUrl != null && !isTrackingEndpoint(this.trackingUrl)) {
            this.trackingUrl = trackingUrl;
        }
        // 플랫폼 도착지에 우리 트래킹 URL이 등록된 경우, 그 값을 랜딩으로 받으면
        // 리다이렉트가 자기 자신을 가리키게 되므로 저장하지 않는다
        if (landingUrl != null && !isTrackingEndpoint(landingUrl)) {
            this.landingUrl = landingUrl;
        }
    }

    private static boolean isTrackingEndpoint(String url) {
        return url != null && url.contains("/api/clicks/track/");
    }
}
