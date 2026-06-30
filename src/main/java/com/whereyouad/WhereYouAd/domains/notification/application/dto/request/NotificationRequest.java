package com.whereyouad.WhereYouAd.domains.notification.application.dto.request;

import java.util.List;

public class NotificationRequest {

    // 전체 알림 설정 DTO
    public record UpdateMaster(
            boolean isMasterEnabled
    ) {}

    // 채널별 알림 설정 DTO
    public record UpdateChannels(
            boolean isBrowserPushEnabled,
            boolean isEmailEnabled,
            boolean isSlackEnabled,
            String slackWebhookUrl,
            boolean isDiscordEnabled,
            String discordWebhookUrl
    ) {}

    // 알림 기준 설정 DTO
    public record UpdateAlerts(
            boolean alertBudget50,
            boolean alertBudget80,
            boolean alertBudget100,
            boolean alertRapidClicks
    ) {}

    // 알림을 받을 멤버 설정 DTO(ADMIN 전용)
    public record UpdateMemberReceive(
            Long membershipId,
            boolean isReceive
    ) {}

    // 멤버 알림 설정 DTO
    public record BulkUpdateMembers(
            List<UpdateMemberReceive> members
    ) {}
}
