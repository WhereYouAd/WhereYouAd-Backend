package com.whereyouad.WhereYouAd.domains.platform.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Timezone;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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
    @Column(name = "status", length = 20)
    @ColumnDefault("'ACTIVE'")
    private PlatformStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private PlatformAccount parentAccount;

    // 수동 연동 해제 요청 시 상태만 DISCONNECTED 로 변경 (실제 데이터 정리는 스케줄러가 수행)
    public void softDelete() {
        this.status = PlatformStatus.DISCONNECTED;
    }

}
