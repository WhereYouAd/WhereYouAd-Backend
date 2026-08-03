package com.whereyouad.WhereYouAd.domains.notification.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "org_member_notification_setting")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrgMemberNotificationSetting extends BaseEntity {

    @Id
    @Column(name = "membership_id")
    private Long membershipId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // PK == FK(식별 관계)
    @JoinColumn(name = "membership_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OrgMember orgMember;

    @Column(name = "is_master_enabled", nullable = false)
    private boolean isMasterEnabled;

    @Column(name = "is_browser_push_enabled", nullable = false)
    private boolean isBrowserPushEnabled;

    @Column(name = "is_email_enabled", nullable = false)
    private boolean isEmailEnabled;

    @Column(name = "alert_clicks", nullable = false)
    @ColumnDefault("false")
    private boolean alertClicks;

    @Column(name = "alert_report", nullable = false)
    @ColumnDefault("false")
    private boolean alertReport;

    public void updateMaster(Boolean isMasterEnabled) {
        if (isMasterEnabled != null) this.isMasterEnabled = isMasterEnabled;
    }

    public void updateChannels(Boolean isBrowserPushEnabled, Boolean isEmailEnabled) {
        if (isBrowserPushEnabled != null) this.isBrowserPushEnabled = isBrowserPushEnabled;
        if (isEmailEnabled != null) this.isEmailEnabled = isEmailEnabled;
    }

    public void updateAlerts(Boolean alertClicks, Boolean alertReport) {
        if (alertClicks != null) this.alertClicks = alertClicks;
        if (alertReport != null) this.alertReport = alertReport;
    }
}
