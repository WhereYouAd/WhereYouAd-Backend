package com.whereyouad.WhereYouAd.domains.notification.application.mapper;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.infrastructure.client.discord.dto.DiscordMessage;
import com.whereyouad.WhereYouAd.infrastructure.client.slack.dto.SlackMessage;

import java.util.List;

public class NotificationConverter {

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
