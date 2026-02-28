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

    private String trackingUrl;

    private String landingUrl;

    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'ON_GOING'")
    private Status status;

    private String cta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_group_id")
    private AdGroup adGroup;
    // @Enumerated(EnumType.STRING)
    // @Column(name = "provider", nullable = false)
    // private Provider provider;

    // private LocalDateTime startDate;
    //
    // private LocalDateTime endDate;
}
