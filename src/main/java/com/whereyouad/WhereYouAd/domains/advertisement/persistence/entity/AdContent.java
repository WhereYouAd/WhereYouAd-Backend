package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.AdStatus;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

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

    private String tracking_url;

    private String landing_url;

    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'ON_GOING'")
    private AdStatus adStatus;

    private String cta;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "provider", nullable = false)
//    private Provider provider;

//    private LocalDateTime startDate;
//
//    private LocalDateTime endDate;
}
