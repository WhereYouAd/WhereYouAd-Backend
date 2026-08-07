package com.whereyouad.WhereYouAd.domains.notification.application.mapper;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.Notification;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.UserNotification;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

import com.whereyouad.WhereYouAd.infrastructure.client.discord.dto.DiscordMessage;
import com.whereyouad.WhereYouAd.infrastructure.client.slack.dto.SlackMessage;

import java.util.List;

public class NotificationConverter {

    // entity -> dto
    public static NotificationResponse.MySettings toMySettings(
            OrgMemberNotificationSetting setting,
            OrgNotificationSetting orgSetting
    ) {
        return new NotificationResponse.MySettings(
                setting.isMasterEnabled(),
                setting.isBrowserPushEnabled(),
                setting.isEmailEnabled(),
                orgSetting != null && orgSetting.isSlackEnabled(),
                orgSetting != null && orgSetting.hasSlack(),
                orgSetting != null && orgSetting.isDiscordEnabled(),
                orgSetting != null && orgSetting.hasDiscord(),
                setting.isAlertClicks(),
                setting.isAlertReport(),
                orgSetting != null && orgSetting.isAlertClicks(),
                orgSetting != null && orgSetting.isAlertReport()
        );
    }

    // entity -> dto
    public static NotificationResponse.MemberSetting toMemberSetting(
            OrgMember member,
            boolean isReceive
    ) {
        return new NotificationResponse.MemberSetting(
                member.getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole().name(),
                isReceive
        );
    }

    // 기본 알림 설정(entity -> dto), 기본값: 전부 OFF (알림 수신은 설정 페이지에서 직접 켜는 opt-in 방식)
    public static OrgMemberNotificationSetting toDefaultMemberSetting(OrgMember member) {
        return OrgMemberNotificationSetting.builder()
                .orgMember(member)
                .isMasterEnabled(false)
                .isBrowserPushEnabled(false)
                .isEmailEnabled(false)
                .alertClicks(false)
                .alertReport(false)
                .build();
    }

    // webhook URL 최초 설정 시 row가 없을 때 빈 상태로 생성
    public static OrgNotificationSetting toDefaultOrgSetting(Organization organization) {
        return OrgNotificationSetting.builder()
                .organization(organization)
                .build();
    }

    public static NotificationResponse.NotificationHistory toNotificationHistory(UserNotification userNotification) {
        Notification notification = userNotification.getNotification();

        return new NotificationResponse.NotificationHistory(
                userNotification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getType(),
                userNotification.isRead()
        );
    }

    // 알림 기록 Slice DTO 변환 (무한 스크롤)
    public static NotificationResponse.NotificationHistoryList toNotificationHistoryList(
            boolean hasNext,
            String nextCursor,
            List<UserNotification> userNotifications
    ) {
        List<NotificationResponse.NotificationHistory> notifications = userNotifications.stream()
                .map(NotificationConverter::toNotificationHistory)
                .toList();

        return new NotificationResponse.NotificationHistoryList(
                hasNext,
                nextCursor,
                notifications
        );
    }

    public static DiscordMessage toDiscordMessage(String title, String message) {
        DiscordMessage.Embed embed = new DiscordMessage.Embed(
                title == null ? "" : title, message == null ? "" : message, 5814783
        );
        return new DiscordMessage("WhereYouAd 알림", List.of(embed));
    }

    public static SlackMessage toSlackMessage(String title, String message) {
        return new SlackMessage("*" + (title == null ? "" : title) + "*\n" + (message == null ? "" : message));
    }
}
