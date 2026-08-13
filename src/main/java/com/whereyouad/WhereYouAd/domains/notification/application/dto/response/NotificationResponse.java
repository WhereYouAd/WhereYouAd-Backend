package com.whereyouad.WhereYouAd.domains.notification.application.dto.response;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationResponse {

    public record MySettings(
            boolean isMasterEnabled,
            boolean isBrowserPushEnabled,
            boolean isEmailEnabled,
            boolean isSlackEnabled,
            boolean isSlackConnected,
            boolean isDiscordEnabled,
            boolean isDiscordConnected,
            boolean alertClicks,
            boolean alertReport,
            boolean orgAlertClicks,
            boolean orgAlertReport
    ) {}

    public record MemberSetting(
            Long membershipId,
            String name,
            String email,
            String role,
            boolean isReceive
    ) {}

    public record MemberSettingList(
            boolean hasNext,
            String nextCursor,
            List<MemberSetting> members
    ) {}

    public record NotificationHistoryList(
            boolean hasNext,
            String nextCursor,
            List<NotificationHistory> notifications
    ) {}

    public record NotificationHistory(
            Long userNotificationId, // UserNotification 의 Id 사용
            String title,
            String message,
            LocalDateTime createdAt,
            NotificationType type,
            boolean isRead
    ) {}
}
