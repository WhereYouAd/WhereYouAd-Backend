package com.whereyouad.WhereYouAd.domains.notification.application.mapper;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushDeliveryResult;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushDeliveryTarget;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.Notification;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationDelivery;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.PushSubscription;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.UserNotification;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

import com.whereyouad.WhereYouAd.infrastructure.client.discord.dto.DiscordMessage;
import com.whereyouad.WhereYouAd.infrastructure.client.slack.dto.SlackMessage;

import java.time.LocalDateTime;
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

    // 조직 대상 알림 발송 시 각 수신자에게 생성되는 in-app 알림 히스토리
    public static UserNotification toUserNotification(Notification notification, OrgMember member) {
        return UserNotification.builder()
                .notification(notification)
                .user(member.getUser())
                .isRead(false)
                .build();
    }

    // 채널별 발송 이력. status=PENDING 으로 시작해 Consumer 가 결과에 따라 SUCCESS/FAILED 로 전이
    public static NotificationDelivery toPendingDelivery(
            Notification notification,
            OrgMember member,
            DeliveryChannel channel
    ) {
        return NotificationDelivery.builder()
                .notification(notification)
                .orgMember(member)
                .channel(channel)
                .status(DeliveryStatus.PENDING)
                .build();
    }

    // 발송 대상 1건 - 구독 매핑됨
    public static PushDeliveryTarget toPushDeliveryTarget(NotificationDelivery delivery, PushSubscription subscription) {
        return new PushDeliveryTarget(
                delivery.getId(),
                delivery.getOrgMember().getId(),
                subscription.getId(),
                subscription.getEndpoint(),
                subscription.getP256dhKey(),
                subscription.getAuthSecret()
        );
    }

    // 발송 대상 1건 - 구독 없는 멤버 (즉시 실패 처리)
    public static PushDeliveryTarget toEmptyPushDeliveryTarget(NotificationDelivery delivery) {
        return new PushDeliveryTarget(delivery.getId(), delivery.getOrgMember().getId(), null, null, null, null);
    }

    // 발송 결과 1건 - status/failReason 조합만 다양. target 에서 delivery/subscription 참조 정보 추출
    public static PushDeliveryResult toPushDeliveryResult(PushDeliveryTarget target, int statusCode, String failReason) {
        return new PushDeliveryResult(target.deliveryId(), target.subscriptionId(), statusCode, failReason);
    }

    // 웹 푸시 Kafka 이벤트 조립 (최초 트리거) - 개별 필드에서 조립
    public static PushNotificationEvent toPushNotificationEvent(
            Long orgId,
            Long notificationId,
            NotificationType type,
            String title,
            String body,
            String linkUrl
    ) {
        return PushNotificationEvent.builder()
                .orgId(orgId)
                .notificationId(notificationId)
                .type(type)
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .build();
    }

    // 웹 푸시 Kafka 이벤트 조립 (재시도 스케줄러) - 저장된 Notification 엔티티에서 재구성
    public static PushNotificationEvent toPushNotificationEvent(Notification n) {
        return toPushNotificationEvent(
                n.getOrganization().getId(),
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLinkUrl());
    }

    // 브라우저 pushManager.subscribe() 결과 -> PushSubscription 엔티티
    public static PushSubscription toPushSubscription(
            OrgMember member,
            NotificationRequest.PushSubscribe request,
            LocalDateTime expirationTime
    ) {
        if (request == null || !request.isValidPushSubscription()) {
            throw new NotificationException(NotificationErrorCode.INVALID_PUSH_SUBSCRIPTION);
        }
        return PushSubscription.builder()
                .orgMember(member)
                .endpoint(request.endpoint())
                .p256dhKey(request.keys().p256dh())
                .authSecret(request.keys().auth())
                .userAgent(request.userAgent())
                .expirationTime(expirationTime)
                .build();
    }

    // 브라우저 푸시 트리거 시 저장할 Notification 엔티티 생성 (title/message null 방어)
    public static Notification toNotification(
            Organization organization,
            NotificationType type,
            String title,
            String message,
            String linkUrl
    ) {
        return Notification.builder()
                .organization(organization)
                .type(type)
                .title(title == null ? "" : title)
                .message(message == null ? "" : message)
                .linkUrl(linkUrl)
                .build();
    }
}
