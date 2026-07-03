package com.whereyouad.WhereYouAd.domains.notification.application.mapper;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
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
                setting.isSlackEnabled(),
                orgSetting != null ? orgSetting.getSlackWebhookUrl() : null,
                setting.isDiscordEnabled(),
                orgSetting != null ? orgSetting.getDiscordWebhookUrl() : null,
                setting.isAlertBudget50(),
                setting.isAlertBudget80(),
                setting.isAlertBudget100(),
                setting.isAlertRapidClicks()
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

    // 기본 알림 설정(entity -> dto), 기본값: 예산 80프로 소진 시 이메일 알림
    public static OrgMemberNotificationSetting toDefaultMemberSetting(OrgMember member) {
        return OrgMemberNotificationSetting.builder()
                .orgMember(member)
                .isMasterEnabled(true)
                .isBrowserPushEnabled(false)
                .isEmailEnabled(true)
                .isSlackEnabled(false)
                .isDiscordEnabled(false)
                .alertBudget50(false)
                .alertBudget80(true)
                .alertBudget100(false)
                .alertRapidClicks(false)
                .build();
    }

    // webhook URL 최초 설정 시 row가 없을 때 빈 상태로 생성
    public static OrgNotificationSetting toDefaultOrgSetting(Organization organization) {
        return OrgNotificationSetting.builder()
                .organization(organization)
                .build();
    }

    public static NotificationResponse.ChannelsListResponse toChannelsListResponse(OrgNotificationSetting setting) {
        return new NotificationResponse.ChannelsListResponse(setting.getOrgId(), setting.hasSlack(), setting.hasDiscord());
    }

    public static NotificationResponse.ChannelsListResponse emptyChannelsResponse(Long orgId) {
        return new NotificationResponse.ChannelsListResponse(orgId, false, false);
    }

    public static DiscordMessage toDiscordMessage(String title, String message) {
        DiscordMessage.Embed embed = new DiscordMessage.Embed(title, message, 5814783); // TODO: 디스코드는 알림 메세지 설정 가능 : 현재는 파랑색
        return new DiscordMessage("WhereYouAd 알림", List.of(embed)); // TODO: 이름을 하드코딩 할지... 아니면 이것도 사용자에게 입력받을지..?
    }

    public static SlackMessage toSlackMessage(String title, String message) {
        return new SlackMessage("*" + title + "*\n" + (message == null ? "" : message));
    }
}
