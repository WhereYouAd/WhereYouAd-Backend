package com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "advertisement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Advertisement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "status", nullable = false)
//    @ColumnDefault("'ON_GOING'")
//    AdStatus 는 ON_GOING, PAUSED, OVER 가 존재
//    join 연산으로 인한 성능 저하 우려 -> 일단은 주석 처리
//    private AdStatus adStatus;
}
