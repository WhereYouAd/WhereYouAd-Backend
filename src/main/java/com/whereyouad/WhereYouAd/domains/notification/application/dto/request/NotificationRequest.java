package com.whereyouad.WhereYouAd.domains.notification.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

public class NotificationRequest {

    // 전체 알림 설정 DTO
    public record UpdateMaster(
            Boolean isMasterEnabled
    ) {}

    // 채널별 알림 설정 DTO
    // slackWebhookUrl/discordWebhookUrl: 필드 미전송 시 변경 없음, null 명시 시 URL 삭제 (ADMIN 전용)
    public record UpdateChannels(
            Boolean isBrowserPushEnabled,
            Boolean isEmailEnabled,
            Boolean isSlackEnabled,
            Optional<String> slackWebhookUrl,
            Boolean isDiscordEnabled,
            Optional<String> discordWebhookUrl
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
            @NotNull(message = "membershipId는 필수입니다.")
            Long membershipId,
            boolean isReceive
    ) {}

    // 멤버 알림 설정 DTO
    public record BulkUpdateMembers(
            @Valid
            @NotEmpty(message = "members는 비어있을 수 없습니다.")
            List<UpdateMemberReceive> members
    ) {}

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
