package com.whereyouad.WhereYouAd.domains.notification.application.mapper;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

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
}
