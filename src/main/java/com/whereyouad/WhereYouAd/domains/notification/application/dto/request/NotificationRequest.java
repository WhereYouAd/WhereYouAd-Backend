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

    // 변경: 멤버 스코프(브라우저/이메일)만 남김. 슬랙/디스코드 웹훅·활성화는 UpdateOrgSettings 로 이동
    public record UpdateChannels(
            Boolean isBrowserPushEnabled,
            Boolean isEmailEnabled
    ) {}

    // 알림 기준 설정 DTO -> 각 멤버
    public record UpdateAlerts(
            Boolean alertRapidClicks,
            Boolean alertBotClicks,
            Boolean alertReport
    ) {}

    // 조직 단위 외부 채널 알림 설정 (ADMIN 전용)
    // 외부 채널 웹훅 연결/활성화 + 외부 채널로 내보낼 알림 종류 설정을 하나의 DTO 로 통합
    public record UpdateOrgSettings(
            Boolean isSlackEnabled,
            String slackWebhookUrl,
            Boolean disconnectSlack,
            Boolean isDiscordEnabled,
            String discordWebhookUrl,
            Boolean disconnectDiscord,
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
