package com.whereyouad.WhereYouAd.domains.notification.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
    private OrgMember orgMember;

    @Column(name = "is_master_enabled", nullable = false)
    private boolean isMasterEnabled;

    @Column(name = "is_browser_push_enabled", nullable = false)
    private boolean isBrowserPushEnabled;

    @Column(name = "is_email_enabled", nullable = false)
    private boolean isEmailEnabled;

    @Column(name = "is_slack_enabled", nullable = false)
    private boolean isSlackEnabled;

    @Column(name = "is_discord_enabled", nullable = false)
    private boolean isDiscordEnabled;

    @Column(name = "alert_budget_50", nullable = false)
    private boolean alertBudget50;

    @Column(name = "alert_budget_80", nullable = false)
    private boolean alertBudget80;

    @Column(name = "alert_budget_100", nullable = false)
    private boolean alertBudget100;

    @Column(name = "alert_rapid_clicks", nullable = false)
    private boolean alertRapidClicks;

    public void updateMaster(boolean isMasterEnabled) {
        this.isMasterEnabled = isMasterEnabled;
    }

    public void updateChannels(boolean isBrowserPushEnabled, boolean isEmailEnabled, boolean isSlackEnabled, boolean isDiscordEnabled) {
        this.isBrowserPushEnabled = isBrowserPushEnabled;
        this.isEmailEnabled = isEmailEnabled;
        this.isSlackEnabled = isSlackEnabled;
        this.isDiscordEnabled = isDiscordEnabled;
    }

    public void updateAlerts(boolean alertBudget50, boolean alertBudget80, boolean alertBudget100, boolean alertRapidClicks) {
        this.alertBudget50 = alertBudget50;
        this.alertBudget80 = alertBudget80;
        this.alertBudget100 = alertBudget100;
        this.alertRapidClicks = alertRapidClicks;
    }
}
