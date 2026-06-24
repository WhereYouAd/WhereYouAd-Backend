package com.whereyouad.WhereYouAd.domains.notification.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public class NotificationRequest {

    public record ChannelSettingRequest(
            String slackWebhookUrl,
            String discordWebhookUrl
    ) {}

    public record TestSend(
            @NotBlank(message = "제목은 필수입니다.")
            String title,
            @NotBlank(message = "메시지는 필수입니다.")
            String message
    ) {}
}
