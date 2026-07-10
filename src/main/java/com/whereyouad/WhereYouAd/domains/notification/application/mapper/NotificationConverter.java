package com.whereyouad.WhereYouAd.domains.notification.application.mapper;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
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
                setting.isAlertRapidClicks(),
                setting.isAlertBotClicks(),
                setting.isAlertReport(),
                orgSetting != null && orgSetting.isAlertRapidClicks(),
                orgSetting != null && orgSetting.isAlertBotClicks(),
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

    // 기본 알림 설정(entity -> dto), 기본값: 마스터 알림 ON, 이메일 수신 ON / 그 외 알림 관련 모두 수신 X
    public static OrgMemberNotificationSetting toDefaultMemberSetting(OrgMember member) {
        return OrgMemberNotificationSetting.builder()
                .orgMember(member)
                .isMasterEnabled(true)
                .isBrowserPushEnabled(false)
                .isEmailEnabled(true)
                .alertRapidClicks(false)
                .alertBotClicks(false)
                .alertReport(false) // TODO : 리포트 알림은 기본값 true 로 하는게 나을지...?
                .build();
    }

    // webhook URL 최초 설정 시 row가 없을 때 빈 상태로 생성
    public static OrgNotificationSetting toDefaultOrgSetting(Organization organization) {
        return OrgNotificationSetting.builder()
                .organization(organization)
                .build();
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
