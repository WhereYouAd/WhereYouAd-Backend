package com.whereyouad.WhereYouAd.domains.notification.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public class NotificationRequest {

    // 전체 알림 설정 DTO
    public record UpdateMaster(
            Boolean isMasterEnabled
    ) {}

    // 채널별 알림 설정 DTO (ADMIN 전용: slackWebhookUrl/discordWebhookUrl, disconnectSlack/disconnectDiscord)
    // 외부 채널 URL: 값 있으면 설정 / disconnectXxx=true 면 삭제(연결 해제) / 둘 다 없으면 변경 없음
    public record UpdateChannels(
            Boolean isBrowserPushEnabled,
            Boolean isEmailEnabled,
            Boolean isSlackEnabled,
            String slackWebhookUrl,
            Boolean disconnectSlack,
            Boolean isDiscordEnabled,
            String discordWebhookUrl,
            Boolean disconnectDiscord
    ) {}

    // 알림 기준 설정 DTO -> 각 멤버
    public record UpdateAlerts(
            Boolean alertRapidClicks,
            Boolean alertBotClicks,
            Boolean alertReport
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

    public record TestSend(
            @NotBlank(message = "제목은 필수입니다.")
            String title,
            @NotBlank(message = "메시지는 필수입니다.")
            String message
    ) {}
}
