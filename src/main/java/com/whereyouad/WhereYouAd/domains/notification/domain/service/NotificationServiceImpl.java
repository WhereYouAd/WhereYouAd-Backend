package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgNotificationSettingRepository;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.discord.DiscordWebhookClient;
import com.whereyouad.WhereYouAd.infrastructure.client.slack.SlackWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final OrgNotificationSettingRepository settingRepository;
    private final DiscordWebhookClient discordClient;
    private final SlackWebhookClient slackClient;
    private final AESUtil aesUtil;

    @Override
    @Transactional(readOnly = true)
    public void sendApiAlarmToOrg(Long orgId, String title, String message) {
        OrgNotificationSetting setting = settingRepository.findById(orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.ORG_NOTIFICATION_SETTING_NOT_FOUND));

        if (!setting.hasSlack() && !setting.hasDiscord()) {
            throw new NotificationException(NotificationErrorCode.NO_CHANNEL_CONFIGURED);
        }

        if (setting.hasSlack()) {
            dispatch(DeliveryChannel.SLACK, setting.getSlackWebhookUrl(), orgId,
                    uri -> slackClient.send(uri, NotificationConverter.toSlackMessage(title, message)));
        }

        if (setting.hasDiscord()) {
            dispatch(DeliveryChannel.DISCORD, setting.getDiscordWebhookUrl(), orgId,
                    uri -> discordClient.send(uri, NotificationConverter.toDiscordMessage(title, message)));
        }
    }

    // 웹훅 URL 복호화 -> 알림 발송 -> 채널별 실패 격리 -> 로깅
    private void dispatch(DeliveryChannel channel, String encryptedUrl, Long orgId, Consumer<URI> sendAction) {
        try {
            String url = new String(aesUtil.decryptAES(encryptedUrl), StandardCharsets.UTF_8).trim();
            sendAction.accept(URI.create(url));
            log.info("[알림 발송 성공] channel={}, orgId={}", channel, orgId);
        } catch (Exception e) {
            log.error("[알림 발송 실패] channel={}, orgId={}, reason={}", channel, orgId, e.getMessage(), e);
        }
    }
}
