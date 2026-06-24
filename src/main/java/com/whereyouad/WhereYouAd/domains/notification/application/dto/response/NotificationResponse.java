package com.whereyouad.WhereYouAd.domains.notification.application.dto.response;

public class NotificationResponse {

    public record ChannelsListResponse(
            Long orgId,
            boolean slackEnabled,
            boolean discordEnabled
    ) {}
}
