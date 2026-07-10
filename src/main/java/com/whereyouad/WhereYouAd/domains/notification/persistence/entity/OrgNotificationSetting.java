package com.whereyouad.WhereYouAd.domains.notification.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.util.StringUtils;

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

    @Column(name = "is_slack_enabled", nullable = false)
    @ColumnDefault("false")
    private boolean isSlackEnabled;

    @Column(name = "is_discord_enabled", nullable = false)
    @ColumnDefault("false")
    private boolean isDiscordEnabled;

    @Column(name = "alert_rapid_clicks", nullable = false)
    @ColumnDefault("false")
    private boolean alertRapidClicks;

    @Column(name = "alert_bot_clicks", nullable = false)
    @ColumnDefault("false")
    private boolean alertBotClicks;

    @Column(name = "alert_report", nullable = false)
    @ColumnDefault("false")
    private boolean alertReport;

    // 조직 단위 외부 채널 알림 토글 업데이트 메서드
    public void updateAlerts(Boolean alertRapidClicks, Boolean alertBotClicks, Boolean alertReport) {
        if (alertRapidClicks != null) this.alertRapidClicks = alertRapidClicks;
        if (alertBotClicks != null) this.alertBotClicks = alertBotClicks;
        if (alertReport != null) this.alertReport = alertReport;
    }

    // 외부 채널 알림 수신 여부 토글 업데이트 메서드
    // 웹훅 URL 이 등록되어 있더라도, 알림 수신 비활성화만 가능하도록 따로 메서드 추가
    public void updateChannelEnabled(Boolean isSlackEnabled, Boolean isDiscordEnabled) {
        if (isSlackEnabled != null) this.isSlackEnabled = isSlackEnabled;
        if (isDiscordEnabled != null) this.isDiscordEnabled = isDiscordEnabled;
    }

    public void updateSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
        if (!StringUtils.hasText(slackWebhookUrl)) {
            this.isSlackEnabled = false; // 웹훅 URL 이 null 로 업데이트 되면 알림 수신 여부도 자동 false 로 전환
        }
    }

    public void updateDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
        if (!StringUtils.hasText(discordWebhookUrl)) {
            this.isDiscordEnabled = false; // 웹훅 URL 이 null 로 업데이트 되면 알림 수신 여부도 자동 false 로 전환
        }
    }

    // 해당 조직에 슬랙 또는 디스코드 웹훅이 연결되어 있는지 확인용 메서드
    public boolean hasSlack() {
        return StringUtils.hasText(slackWebhookUrl);
    }

    public boolean hasDiscord() {
        return StringUtils.hasText(discordWebhookUrl);
    }
}
