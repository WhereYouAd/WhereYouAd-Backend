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
            boolean alertRapidClicks,
            boolean alertBotClicks,
            boolean alertReport,
            boolean orgAlertRapidClicks,   // 조직 외부채널 발송 설정 (ADMIN 설정)
            boolean orgAlertBotClicks,
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

}
