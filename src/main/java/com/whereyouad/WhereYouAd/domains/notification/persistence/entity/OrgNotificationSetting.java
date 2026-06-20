package com.whereyouad.WhereYouAd.domains.notification.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_notification_setting")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrgNotificationSetting extends BaseEntity {

    @Id
    @Column(name = "org_id")
    private Long orgId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // PK == FK(식별 관계)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @Column(name = "slack_webhook_url", length = 512)
    private String slackWebhookUrl;

    @Column(name = "discord_webhook_url", length = 512)
    private String discordWebhookUrl;
}
