package com.whereyouad.WhereYouAd.domains.notification.application.dto.response;

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
            boolean alertBudget50,
            boolean alertBudget80,
            boolean alertBudget100,
            boolean alertRapidClicks
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

}
