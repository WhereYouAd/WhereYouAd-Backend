package com.whereyouad.WhereYouAd.domains.notification.application.dto.request;

import java.util.List;

public class NotificationRequest {

    // 전체 알림 설정 DTO
    public record UpdateMaster(
            Boolean isMasterEnabled
    ) {}

    // 채널별 알림 설정 DTO
    public record UpdateChannels(
            Boolean isBrowserPushEnabled,
            Boolean isEmailEnabled,
            Boolean isSlackEnabled,
            String slackWebhookUrl,
            Boolean isDiscordEnabled,
            String discordWebhookUrl
    ) {}

    // 알림 기준 설정 DTO
    public record UpdateAlerts(
            Boolean alertBudget50,
            Boolean alertBudget80,
            Boolean alertBudget100,
            Boolean alertRapidClicks
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
